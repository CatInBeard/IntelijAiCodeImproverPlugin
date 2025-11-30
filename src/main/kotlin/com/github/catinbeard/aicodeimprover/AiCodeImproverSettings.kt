package com.github.catinbeard.aicodeimprover

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class AiCodeImproverSettings : Configurable {

    private val properties = PropertiesComponent.getInstance()

    private val lmStudioUrlKey = "aiCodeImprover.lmStudioUrl"
    private val systemPromptKey = "aiCodeImprover.systemPrompt"
    private val selectedModelKey = "aiCodeImprover.selectedModel"
    private val temperatureKey = "aiCodeImprover.temperature"

    private var lmStudioUrl: String
        get() = properties.getValue(lmStudioUrlKey, "http://localhost:1234/api/v0")
        set(value) = properties.setValue(lmStudioUrlKey, value)

    private var systemPrompt: String
        get() = properties.getValue(systemPromptKey, "Answer only code, without markdown formatting, ONLY CODE")
        set(value) = properties.setValue(systemPromptKey, value)

    private var selectedModel: String
        get() = properties.getValue(selectedModelKey, "")
        set(value) = properties.setValue(selectedModelKey, value)

    private var temperature: Double
        get() = properties.getValue(temperatureKey, "0.1").toDoubleOrNull() ?: 0.1
        set(value) = properties.setValue(temperatureKey, value.toString())

    private var urlField: TextFieldWithBrowseButton? = null
    private var promptArea: JBTextArea? = null
    private var modelCombo: ComboBox<String>? = null
    private var temperatureField: JBTextField? = null
    private var statusLabel: HyperlinkLabel? = null

    override fun createComponent(): JComponent {
        urlField = TextFieldWithBrowseButton()
        urlField!!.text = lmStudioUrl

        promptArea = JBTextArea(systemPrompt, 10, 50)
        promptArea!!.lineWrap = true
        promptArea!!.wrapStyleWord = true

        modelCombo = ComboBox<String>()
        modelCombo!!.isEditable = true
        modelCombo!!.selectedItem = selectedModel

        temperatureField = JBTextField(temperature.toString())

        statusLabel = HyperlinkLabel()
        statusLabel!!.setText("Loading models...")

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val client = LMStudioClient(lmStudioUrl)
                val models = client.getModels()
                ApplicationManager.getApplication().invokeLater {
                    if (models.isNotEmpty()) {
                        modelCombo!!.removeAllItems()
                        models.forEach { modelCombo!!.addItem(it) }
                        val newModel = if (selectedModel.isEmpty()) models.first() else selectedModel
                        modelCombo!!.selectedItem = newModel
                        statusLabel!!.setText("")
                    } else {
                        statusLabel!!.setTextWithHyperlink("You need to download models in LMStudio")
                        statusLabel!!.setHyperlinkTarget("https://lmstudio.ai/docs")
                    }
                }
            } catch (e: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    statusLabel!!.setTextWithHyperlink("Unable to connect to LMStudio. Check URL and ensure LMStudio is running.")
                    statusLabel!!.setHyperlinkTarget("https://lmstudio.ai/docs")
                }
            }
        }

        return FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("LMStudio URL:"), urlField!!)
            .addLabeledComponent(JBLabel("System Prompt:"), JBScrollPane(promptArea!!))
            .addLabeledComponent(JBLabel("Model:"), modelCombo!!)
            .addLabeledComponent(JBLabel("Temperature:"), temperatureField!!)
            .addComponent(statusLabel!!)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    override fun isModified(): Boolean {
        return urlField?.text != lmStudioUrl ||
                promptArea?.text != systemPrompt ||
                modelCombo?.selectedItem != selectedModel ||
                temperatureField?.text?.toDoubleOrNull() != temperature
    }

    override fun apply() {
        lmStudioUrl = urlField?.text ?: lmStudioUrl
        systemPrompt = promptArea?.text ?: systemPrompt
        selectedModel = modelCombo?.selectedItem as? String ?: selectedModel
        temperature = temperatureField?.text?.toDoubleOrNull() ?: temperature
    }

    override fun reset() {
        urlField?.text = lmStudioUrl
        promptArea?.text = systemPrompt
        modelCombo?.selectedItem = selectedModel
        temperatureField?.text = temperature.toString()
    }

    override fun getDisplayName(): String = "AI Code Improver"
}