package org.gcb.blah.mybatis

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.openapi.components.State

@Service(Service.Level.PROJECT)
// 2. 定义存储文件的名称
@State(name = "org.gcb.blah.mybatis.PluginSettingState", storages = [Storage("MyBatisPluginSettings.xml")])
class PluginSettingState: PersistentStateComponent<PluginSettingState> {
    // 保存工具类的全限定名，例如 "com.example.utils.SqlExecutor"
    var toolClassName: String = ""
    var isLooking4NativeMapper: Boolean = false

    override fun getState(): PluginSettingState {
        return this
    }

    override fun loadState(state: PluginSettingState) {
        this.toolClassName = state.toolClassName
        this.isLooking4NativeMapper = state.isLooking4NativeMapper
    }

    companion object {
        fun getInstance(project: Project): PluginSettingState {
            return project.getService(PluginSettingState::class.java)
        }
    }
}