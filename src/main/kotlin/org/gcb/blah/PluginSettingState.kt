package org.gcb.blah

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.openapi.components.State

@Service(Service.Level.PROJECT)
// 2. 定义存储文件的名称
@State(name = "org.gcb.blah.PluginSettingState", storages = [Storage("MyBatisPluginSettings.xml")])
class PluginSettingState {
    // 保存工具类的全限定名，例如 "com.example.utils.SqlExecutor"
    var toolClassName: String = ""

    fun getState(): PluginSettingState {
        return this
    }

    fun loadState(state: PluginSettingState) {
        this.toolClassName = state.toolClassName
    }

    companion object {
        fun getInstance(project: Project): PluginSettingState {
            return project.getService(PluginSettingState::class.java)
        }
    }
}