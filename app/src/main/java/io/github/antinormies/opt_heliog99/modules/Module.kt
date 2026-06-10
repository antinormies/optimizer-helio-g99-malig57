package io.github.antinormies.opt_heliog99.modules

import io.github.antinormies.opt_heliog99.config.AppConfig

interface Module {
    val name: String
    fun getCommands(profile: AppConfig.Profile, vulkan: Boolean): List<String>
}
