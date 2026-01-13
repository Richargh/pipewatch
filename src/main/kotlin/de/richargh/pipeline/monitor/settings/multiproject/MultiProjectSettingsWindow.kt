@file:Suppress("ktlint:standard:function-naming")

package de.richargh.pipeline.monitor.settings.multiproject

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import de.richargh.pipeline.monitor.gitlab.api.GitLabApiException
import de.richargh.pipeline.monitor.gitlab.api.GitLabClient
import de.richargh.pipeline.monitor.settings.ConnectionTestResult
import de.richargh.pipeline.monitor.settings.RefreshInterval
import de.richargh.pipeline.monitor.settings.SettingsRepository
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun MultiProjectSettingsWindow(
    settingsRepository: SettingsRepository,
    multiProjectRepository: MultiProjectRepository,
    onClose: () -> Unit,
    onSave: () -> Unit,
) {
    Window(
        onCloseRequest = onClose,
        title = "GitLab Pipeline Monitor Settings",
        state = WindowState(width = 550.dp, height = 700.dp),
        resizable = true,
    ) {
        MaterialTheme {
            MultiProjectSettingsContent(
                settingsRepository = settingsRepository,
                multiProjectRepository = multiProjectRepository,
                onClose = onClose,
                onSave = onSave,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiProjectSettingsContent(
    settingsRepository: SettingsRepository,
    multiProjectRepository: MultiProjectRepository,
    onClose: () -> Unit,
    onSave: () -> Unit,
) {
    var projects by remember { mutableStateOf(multiProjectRepository.getAllProjects()) }
    var activeProjectId by remember { mutableStateOf(multiProjectRepository.activeProjectId) }
    var selectedProject by remember { mutableStateOf(projects.find { it.id == activeProjectId }) }

    var refreshInterval by remember { mutableStateOf(settingsRepository.refreshInterval) }
    var notificationsEnabled by remember { mutableStateOf(settingsRepository.notificationsEnabled) }
    var branchFilter by remember { mutableStateOf(settingsRepository.branchFilter ?: "") }

    var showAddProjectDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var projectToDelete by remember { mutableStateOf<ProjectConfig?>(null) }
    var projectsExpanded by remember { mutableStateOf(false) }
    var refreshIntervalExpanded by remember { mutableStateOf(false) }

    var connectionTestResult by remember { mutableStateOf<ConnectionTestResult?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Project",
                style = MaterialTheme.typography.titleMedium,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ExposedDropdownMenuBox(
                    expanded = projectsExpanded,
                    onExpandedChange = { projectsExpanded = !projectsExpanded },
                    modifier = Modifier.weight(1f),
                ) {
                    OutlinedTextField(
                        value = selectedProject?.displayName ?: "Select a project",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Active Project") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = projectsExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )

                    ExposedDropdownMenu(
                        expanded = projectsExpanded,
                        onDismissRequest = { projectsExpanded = false },
                    ) {
                        if (projects.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No projects configured") },
                                onClick = { projectsExpanded = false },
                                enabled = false,
                            )
                        } else {
                            projects.forEach { project ->
                                DropdownMenuItem(
                                    text = { Text(project.displayName) },
                                    onClick = {
                                        selectedProject = project
                                        activeProjectId = project.id
                                        connectionTestResult = null
                                        projectsExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = { showAddProjectDialog = true },
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Project")
                }

                if (selectedProject != null) {
                    IconButton(
                        onClick = {
                            projectToDelete = selectedProject
                            showDeleteConfirmation = true
                        },
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Project",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            selectedProject?.let { project ->
                val token = multiProjectRepository.getToken(project.tokenId)

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Connection Details",
                    style = MaterialTheme.typography.titleMedium,
                )

                OutlinedTextField(
                    value = project.gitLabUrl,
                    onValueChange = { },
                    label = { Text("GitLab URL") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = project.projectPath,
                    onValueChange = { },
                    label = { Text("Project Path") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = token?.accessToken?.let { "•".repeat(minOf(it.length, 20)) } ?: "Token not found",
                    onValueChange = { },
                    label = { Text("Access Token") },
                    readOnly = true,
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
                                connectionTestResult = testProjectConnection(
                                    project.gitLabUrl,
                                    token?.accessToken ?: ""
                                )
                                isTestingConnection = false
                            }
                        },
                        enabled = token != null && !isTestingConnection,
                    ) {
                        Text(if (isTestingConnection) "Testing..." else "Test Connection")
                    }

                    connectionTestResult?.let { result ->
                        Text(
                            text = result.message,
                            color = when (result) {
                                is ConnectionTestResult.Success -> MaterialTheme.colorScheme.primary
                                is ConnectionTestResult.Failure -> MaterialTheme.colorScheme.error
                            },
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
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
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
                        multiProjectRepository.activeProjectId = activeProjectId
                        settingsRepository.refreshInterval = refreshInterval
                        settingsRepository.notificationsEnabled = notificationsEnabled
                        settingsRepository.branchFilter = branchFilter.ifBlank { null }

                        // Sync selected project to legacy settings for compatibility
                        selectedProject?.let { project ->
                            val token = multiProjectRepository.getToken(project.tokenId)
                            settingsRepository.gitLabUrl = project.gitLabUrl
                            settingsRepository.accessToken = token?.accessToken
                            settingsRepository.selectedProjectId = project.projectId
                            settingsRepository.selectedProjectName = project.name
                        }

                        onSave()
                        onClose()
                    },
                    enabled = selectedProject != null,
                ) {
                    Text("Save")
                }
            }
        }
    }

    if (showAddProjectDialog) {
        AddProjectDialog(
            multiProjectRepository = multiProjectRepository,
            onDismiss = { showAddProjectDialog = false },
            onProjectAdded = { newProject ->
                projects = multiProjectRepository.getAllProjects()
                selectedProject = newProject
                activeProjectId = newProject.id
                showAddProjectDialog = false
            },
        )
    }

    if (showDeleteConfirmation && projectToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmation = false
                projectToDelete = null
            },
            title = { Text("Delete Project") },
            text = { Text("Are you sure you want to delete '${projectToDelete?.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        projectToDelete?.let { project ->
                            multiProjectRepository.deleteProject(project.id)
                            multiProjectRepository.deleteOrphanedTokens()
                            projects = multiProjectRepository.getAllProjects()
                            if (selectedProject?.id == project.id) {
                                selectedProject = projects.firstOrNull()
                                activeProjectId = selectedProject?.id
                            }
                        }
                        showDeleteConfirmation = false
                        projectToDelete = null
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        projectToDelete = null
                    },
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProjectDialog(
    multiProjectRepository: MultiProjectRepository,
    onDismiss: () -> Unit,
    onProjectAdded: (ProjectConfig) -> Unit,
) {
    var urlInput by remember { mutableStateOf("") }
    var accessToken by remember { mutableStateOf("") }
    var parsedUrl by remember { mutableStateOf<ParsedGitLabUrl?>(null) }
    var existingToken by remember { mutableStateOf<TokenConfig?>(null) }
    var useExistingToken by remember { mutableStateOf(true) }
    var isValidating by remember { mutableStateOf(false) }
    var validationResult by remember { mutableStateOf<ProjectValidationResult?>(null) }

    val parser = remember { GitLabUrlParser() }
    val coroutineScope = rememberCoroutineScope()

    // Parse URL whenever input changes
    val currentParsed = parser.parse(urlInput)
    if (currentParsed != parsedUrl) {
        parsedUrl = currentParsed
        existingToken = currentParsed?.let { multiProjectRepository.findTokenForGitLabUrl(it.gitLabUrl) }
        useExistingToken = existingToken != null
        validationResult = null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Project") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("GitLab Pipeline URL") },
                    placeholder = { Text("https://gitlab.example.com/group/project/-/pipelines") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = urlInput.isNotEmpty() && parsedUrl == null,
                    supportingText = if (urlInput.isNotEmpty() && parsedUrl == null) {
                        { Text("Invalid GitLab URL") }
                    } else null,
                )

                parsedUrl?.let { parsed ->
                    Text(
                        text = "GitLab: ${parsed.gitLabUrl}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Project: ${parsed.projectPath}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    existingToken?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = useExistingToken,
                                onCheckedChange = { useExistingToken = it },
                            )
                            Text("Use existing token for ${parsed.gitLabUrl}")
                        }
                    }

                    if (existingToken == null || !useExistingToken) {
                        OutlinedTextField(
                            value = accessToken,
                            onValueChange = { accessToken = it },
                            label = { Text("Personal Access Token") },
                            placeholder = { Text("glpat-xxxxxxxxxxxx") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                }

                validationResult?.let { result ->
                    Text(
                        text = result.message,
                        color = when (result) {
                            is ProjectValidationResult.Success -> MaterialTheme.colorScheme.primary
                            is ProjectValidationResult.Error -> MaterialTheme.colorScheme.error
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    parsedUrl?.let { parsed ->
                        isValidating = true
                        validationResult = null
                        coroutineScope.launch {
                            val tokenToUse = if (useExistingToken && existingToken != null) {
                                existingToken!!.accessToken
                            } else {
                                accessToken
                            }

                            val result = validateAndAddProject(
                                parsed = parsed,
                                token = tokenToUse,
                                existingToken = if (useExistingToken) existingToken else null,
                                multiProjectRepository = multiProjectRepository,
                            )

                            validationResult = result
                            isValidating = false

                            if (result is ProjectValidationResult.Success) {
                                onProjectAdded(result.project)
                            }
                        }
                    }
                },
                enabled = parsedUrl != null &&
                    (useExistingToken && existingToken != null || accessToken.isNotEmpty()) &&
                    !isValidating,
            ) {
                Text(if (isValidating) "Validating..." else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

sealed class ProjectValidationResult(val message: String) {
    class Success(message: String, val project: ProjectConfig) : ProjectValidationResult(message)
    class Error(message: String) : ProjectValidationResult(message)
}

private suspend fun validateAndAddProject(
    parsed: ParsedGitLabUrl,
    token: String,
    existingToken: TokenConfig?,
    multiProjectRepository: MultiProjectRepository,
): ProjectValidationResult {
    val client = GitLabClient(parsed.gitLabUrl, token)
    return try {
        // First test connection
        val connectionResult = client.testConnection()
        if (connectionResult.isFailure) {
            val exception = connectionResult.exceptionOrNull()
            return when (exception) {
                is GitLabApiException.Unauthorized -> ProjectValidationResult.Error("Invalid access token")
                is GitLabApiException.Forbidden -> ProjectValidationResult.Error("Access denied")
                is GitLabApiException.NetworkError -> ProjectValidationResult.Error("Cannot connect to GitLab")
                else -> ProjectValidationResult.Error("Connection failed: ${exception?.message}")
            }
        }

        // Then look up the project
        val project = client.getProjectByPath(parsed.projectPath)
            ?: return ProjectValidationResult.Error("Project not found: ${parsed.projectPath}")

        // Save or reuse token
        val tokenId = if (existingToken != null) {
            existingToken.id
        } else {
            val newTokenId = UUID.randomUUID().toString()
            multiProjectRepository.saveToken(
                TokenConfig(
                    id = newTokenId,
                    gitLabUrl = parsed.gitLabUrl,
                    accessToken = token,
                )
            )
            newTokenId
        }

        // Save project config
        val projectConfig = ProjectConfig(
            id = UUID.randomUUID().toString(),
            name = project.pathWithNamespace,
            gitLabUrl = parsed.gitLabUrl,
            projectPath = parsed.projectPath,
            projectId = project.id,
            tokenId = tokenId,
        )
        multiProjectRepository.saveProject(projectConfig)

        ProjectValidationResult.Success("Project added successfully", projectConfig)
    } catch (e: Exception) {
        ProjectValidationResult.Error("Error: ${e.message}")
    } finally {
        client.close()
    }
}

private suspend fun testProjectConnection(
    gitLabUrl: String,
    accessToken: String,
): ConnectionTestResult {
    val client = GitLabClient(gitLabUrl, accessToken)
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
