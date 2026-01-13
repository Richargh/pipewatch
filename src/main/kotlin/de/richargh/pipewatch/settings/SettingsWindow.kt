@file:Suppress("ktlint:standard:function-naming")

package de.richargh.pipewatch.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import de.richargh.pipewatch.gitlab.api.GitLabApiException
import de.richargh.pipewatch.gitlab.api.GitLabClient
import de.richargh.pipewatch.gitlab.api.Project
import kotlinx.coroutines.launch

@Composable
fun SettingsWindow(
    settingsRepository: SettingsRepository,
    onClose: () -> Unit,
    onSave: () -> Unit,
) {
    Window(
        onCloseRequest = onClose,
        title = "GitLab Pipeline Monitor Settings",
        state = WindowState(width = 500.dp, height = 600.dp),
        resizable = true,
    ) {
        MaterialTheme {
            SettingsContent(
                settingsRepository = settingsRepository,
                onClose = onClose,
                onSave = onSave,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    settingsRepository: SettingsRepository,
    onClose: () -> Unit,
    onSave: () -> Unit,
) {
    var gitLabUrl by remember { mutableStateOf(settingsRepository.gitLabUrl ?: "") }
    var accessToken by remember { mutableStateOf(settingsRepository.accessToken ?: "") }
    var selectedProjectId by remember { mutableStateOf(settingsRepository.selectedProjectId) }
    var selectedProjectName by remember { mutableStateOf(settingsRepository.selectedProjectName ?: "") }
    var refreshInterval by remember { mutableStateOf(settingsRepository.refreshInterval) }
    var notificationsEnabled by remember { mutableStateOf(settingsRepository.notificationsEnabled) }
    var branchFilter by remember { mutableStateOf(settingsRepository.branchFilter ?: "") }

    var connectionTestResult by remember { mutableStateOf<ConnectionTestResult?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var isLoadingProjects by remember { mutableStateOf(false) }
    var projectsExpanded by remember { mutableStateOf(false) }
    var refreshIntervalExpanded by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    val urlError =
        if (gitLabUrl.isNotEmpty() && !SettingsRepository.isValidGitLabUrl(gitLabUrl)) {
            "Invalid URL format"
        } else {
            null
        }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "GitLab Connection",
                style = MaterialTheme.typography.titleMedium,
            )

            OutlinedTextField(
                value = gitLabUrl,
                onValueChange = { gitLabUrl = it },
                label = { Text("GitLab URL") },
                placeholder = { Text("https://gitlab.example.com") },
                isError = urlError != null,
                supportingText = urlError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = accessToken,
                onValueChange = { accessToken = it },
                label = { Text("Personal Access Token") },
                placeholder = { Text("glpat-xxxxxxxxxxxx") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = {
                        isTestingConnection = true
                        connectionTestResult = null
                        coroutineScope.launch {
                            connectionTestResult = testConnection(gitLabUrl, accessToken)
                            isTestingConnection = false
                            if (connectionTestResult is ConnectionTestResult.Success) {
                                loadProjects(gitLabUrl, accessToken) { loadedProjects ->
                                    projects = loadedProjects
                                }
                            }
                        }
                    },
                    enabled = gitLabUrl.isNotEmpty() && accessToken.isNotEmpty() && !isTestingConnection,
                ) {
                    Text(if (isTestingConnection) "Testing..." else "Test Connection")
                }

                connectionTestResult?.let { result ->
                    Text(
                        text = result.message,
                        color =
                            when (result) {
                                is ConnectionTestResult.Success -> MaterialTheme.colorScheme.primary
                                is ConnectionTestResult.Failure -> MaterialTheme.colorScheme.error
                            },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Project Selection",
                style = MaterialTheme.typography.titleMedium,
            )

            ExposedDropdownMenuBox(
                expanded = projectsExpanded,
                onExpandedChange = {
                    projectsExpanded = !projectsExpanded
                    if (projectsExpanded && projects.isEmpty() && gitLabUrl.isNotEmpty() && accessToken.isNotEmpty()) {
                        isLoadingProjects = true
                        coroutineScope.launch {
                            loadProjects(gitLabUrl, accessToken) { loadedProjects ->
                                projects = loadedProjects
                                isLoadingProjects = false
                            }
                        }
                    }
                },
            ) {
                OutlinedTextField(
                    value = selectedProjectName,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Select Project") },
                    placeholder = { Text(if (isLoadingProjects) "Loading..." else "Choose a project") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = projectsExpanded) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                )

                ExposedDropdownMenu(
                    expanded = projectsExpanded,
                    onDismissRequest = { projectsExpanded = false },
                ) {
                    projects.forEach { project ->
                        DropdownMenuItem(
                            text = { Text(project.pathWithNamespace) },
                            onClick = {
                                selectedProjectId = project.id
                                selectedProjectName = project.pathWithNamespace
                                projectsExpanded = false
                            },
                        )
                    }
                    if (projects.isEmpty() && !isLoadingProjects) {
                        DropdownMenuItem(
                            text = { Text("No projects found. Test connection first.") },
                            onClick = { projectsExpanded = false },
                            enabled = false,
                        )
                    }
                }
            }

            OutlinedTextField(
                value = branchFilter,
                onValueChange = { branchFilter = it },
                label = { Text("Branch Filter (optional)") },
                placeholder = { Text("main") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Preferences",
                style = MaterialTheme.typography.titleMedium,
            )

            ExposedDropdownMenuBox(
                expanded = refreshIntervalExpanded,
                onExpandedChange = { refreshIntervalExpanded = !refreshIntervalExpanded },
            ) {
                OutlinedTextField(
                    value = refreshInterval.displayName,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Refresh Interval") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = refreshIntervalExpanded) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                )

                ExposedDropdownMenu(
                    expanded = refreshIntervalExpanded,
                    onDismissRequest = { refreshIntervalExpanded = false },
                ) {
                    RefreshInterval.entries.forEach { interval ->
                        DropdownMenuItem(
                            text = { Text(interval.displayName) },
                            onClick = {
                                refreshInterval = interval
                                refreshIntervalExpanded = false
                            },
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it },
                )
                Text("Enable notifications for pipeline failures")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                OutlinedButton(onClick = onClose) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        settingsRepository.gitLabUrl = gitLabUrl.ifBlank { null }
                        settingsRepository.accessToken = accessToken.ifBlank { null }
                        selectedProjectId?.let {
                            settingsRepository.setSelectedProject(SelectedProject(it, selectedProjectName))
                        } ?: settingsRepository.clearSelectedProject()
                        settingsRepository.refreshInterval = refreshInterval
                        settingsRepository.notificationsEnabled = notificationsEnabled
                        settingsRepository.branchFilter = branchFilter.ifBlank { null }
                        onSave()
                        onClose()
                    },
                    enabled = gitLabUrl.isNotEmpty() && accessToken.isNotEmpty() && selectedProjectId != null,
                ) {
                    Text("Save")
                }
            }
        }
    }
}

sealed class ConnectionTestResult(val message: String) {
    class Success(message: String = "Connection successful") : ConnectionTestResult(message)

    class Failure(message: String) : ConnectionTestResult(message)
}

private suspend fun testConnection(
    gitLabUrl: String,
    accessToken: String,
): ConnectionTestResult {
    val client = GitLabClient(gitLabUrl.trimEnd('/'), accessToken.trim())
    return try {
        val result = client.testConnection()
        if (result.isSuccess) {
            ConnectionTestResult.Success()
        } else {
            val exception = result.exceptionOrNull()
            when (exception) {
                is GitLabApiException.Unauthorized -> ConnectionTestResult.Failure("Invalid access token")
                is GitLabApiException.Forbidden -> ConnectionTestResult.Failure("Access denied")
                is GitLabApiException.NetworkError -> ConnectionTestResult.Failure("Cannot connect to GitLab")
                else -> ConnectionTestResult.Failure("Connection failed: ${exception?.message}")
            }
        }
    } catch (e: Exception) {
        ConnectionTestResult.Failure("Error: ${e.message}")
    } finally {
        client.close()
    }
}

private suspend fun loadProjects(
    gitLabUrl: String,
    accessToken: String,
    onResult: (List<Project>) -> Unit,
) {
    val client = GitLabClient(gitLabUrl.trimEnd('/'), accessToken.trim())
    try {
        val projects = client.getProjects(perPage = 100)
        onResult(projects)
    } catch (e: Exception) {
        onResult(emptyList())
    } finally {
        client.close()
    }
}
