package com.github.catinbeard.aicodeimprover

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.NlsSafe


class ContextMenuAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.getData(CommonDataKeys.PROJECT) ?: return
        val document = editor.document

        val primaryCaret = editor.caretModel.primaryCaret
        val start = primaryCaret.selectionStart
        val end = primaryCaret.selectionEnd

        val selectedText = editor.selectionModel.selectedText ?: return

        val properties = PropertiesComponent.getInstance()
        val lastPrompt = properties.getValue("aiCodeImprover.lastPrompt", "Improve this code by adding useful comments and optimizing it.")

        val prompt = Messages.showMultilineInputDialog(
            project,
            "Enter instructions for AI to improve the selected code:",
            "AI Code Improvement",
            lastPrompt,
            null,
            null
        ) ?: return

        properties.setValue("aiCodeImprover.lastPrompt", prompt)

        val notification = NotificationGroupManager.getInstance().getNotificationGroup("AI Code Improver")
            .createNotification("AI is thinking...", NotificationType.INFORMATION)
        notification.notify(project)

        ApplicationManager.getApplication().executeOnPooledThread {
            val improved = improveText(selectedText, prompt)
            ApplicationManager.getApplication().invokeLater {
                notification.expire()
                val originalContent = DiffContentFactory.getInstance().create(selectedText)
                val improvedContent = DiffContentFactory.getInstance().create(improved)
                val request = SimpleDiffRequest("AI Code Improvement", originalContent, improvedContent, "Original", "Improved")
                DiffManager.getInstance().showDiff(project, request)
                val result = Messages.showYesNoCancelDialog(
                    project,
                    "Apply the AI improvements to the code?",
                    "AI Code Improvement",
                    "Apply",
                    "Reject",
                    "Reject and Regenerate",
                    Messages.getQuestionIcon()
                )
                if (result == Messages.YES) {
                    WriteCommandAction.runWriteCommandAction(project) {
                        document.replaceString(start, end, improved)
                    }
                    val successNotification = NotificationGroupManager.getInstance().getNotificationGroup("AI Code Improver")
                        .createNotification("Code improved!", NotificationType.INFORMATION)
                    successNotification.notify(project)
                } else if (result == Messages.CANCEL) {
                    val reason = Messages.showInputDialog(
                        project,
                        "Enter reason for rejection:",
                        "Regenerate Code",
                        Messages.getQuestionIcon()
                    )
                    if (reason != null) {
                        val newNotification = NotificationGroupManager.getInstance().getNotificationGroup("AI Code Improver")
                            .createNotification("AI is regenerating...", NotificationType.INFORMATION)
                        newNotification.notify(project)
                        ApplicationManager.getApplication().executeOnPooledThread {
                            val newPrompt = "$prompt\n\nPrevious attempt was rejected because: $reason\n\nPlease improve the code accordingly."
                            val newImproved = improveText(selectedText, newPrompt)
                            ApplicationManager.getApplication().invokeLater {
                                newNotification.expire()
                                val newOriginalContent = DiffContentFactory.getInstance().create(selectedText)
                                val newImprovedContent = DiffContentFactory.getInstance().create(newImproved)
                                val newRequest = SimpleDiffRequest("AI Code Improvement (Regenerated)", newOriginalContent, newImprovedContent, "Original", "Regenerated")
                                DiffManager.getInstance().showDiff(project, newRequest)
                                val newResult = Messages.showYesNoCancelDialog(
                                    project,
                                    "Apply the regenerated AI improvements?",
                                    "AI Code Improvement",
                                    "Apply",
                                    "Reject",
                                    "Reject & Regenerate",
                                    Messages.getQuestionIcon()
                                )
                                if (newResult == Messages.YES) {
                                    WriteCommandAction.runWriteCommandAction(project) {
                                        document.replaceString(start, end, newImproved)
                                    }
                                    val successNotification = NotificationGroupManager.getInstance().getNotificationGroup("AI Code Improver")
                                        .createNotification("Code improved!", NotificationType.INFORMATION)
                                    successNotification.notify(project)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun improveText(code: @NlsSafe String?, prompt: String): String {
        val properties = PropertiesComponent.getInstance()
        val lmStudioUrl = properties.getValue("aiCodeImprover.lmStudioUrl", "http://localhost:1234/api/v0")
        val selectedModel = properties.getValue("aiCodeImprover.selectedModel", "")
        val systemPrompt = properties.getValue("aiCodeImprover.systemPrompt", "You receive code, add usefully but dummy comments before every line of code. Answer only code, without markdown formatting, ONLY CODE")
        val temperature = properties.getValue("aiCodeImprover.temperature", "0.1").toDoubleOrNull() ?: 0.1
        val client = LMStudioClient(lmStudioUrl)

        val response = client.getSimpleAnswer(
            selectedModel,
            systemPrompt,
            "$prompt\n\n${code ?: ""}",
            temperature)

        val regex = Regex("<think>.*?</think>", RegexOption.MULTILINE)
        val responseNoThink = response.replace("\r", "").replace(regex, "")

        return responseNoThink
    }
}
