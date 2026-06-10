# Mali-G57 MC2 — Vulkan & GPU Notes

## Device Info

| Field | Value |
|-------|-------|
| GPU | ARM Mali-G57 MC2 |
| Core Config | 2 shader cores |
| Architecture | Valhall (first-gen Valhall) |
| Driver | vulkan.mali.so (MediaTek mt6789 variant) |
| API | Vulkan 1.1, OpenGL ES 3.2 |
| Max Frequency | Likely 1 GHz (kernel-locked, cannot verify without root) |
| Compute | Vulkan compute level 0 (basic compute support) |

## Vulkan Capabilities

### API Version
- Vulkan 1.1 (0x00401000 = 4198400)
- Hardware level 1 (fully featured GPU)
- Compute level 0 (basic compute support)

### Vulkan Driver Stack
- `/vendor/lib64/hw/vulkan.mali.so` → symlink to `mt6789/vulkan.mali.so`
- `/vendor/lib/hw/vulkan.mali.so` → symlink to `mt6789/vulkan.mali.so`
- `/system/lib64/libvulkan.so` (loader)
- `/system/lib/libvulkan.so` (loader 32-bit)

### Extensions (Expected for Mali Valhall)
Based on ARM Mali-G57 Valhall architecture and common Mali Vulkan driver extensions:

**Device Extensions (likely available):**
- VK_KHR_8bit_storage
- VK_KHR_16bit_storage
- VK_KHR_bind_memory2
- VK_KHR_create_renderpass2
- VK_KHR_dedicated_allocation
- VK_KHR_descriptor_update_template
- VK_KHR_device_group
- VK_KHR_draw_indirect_count
- VK_KHR_driver_properties
- VK_KHR_get_memory_requirements2
- VK_KHR_image_format_list
- VK_KHR_maintenance1/2/3
- VK_KHR_multiview
- VK_KHR_push_descriptor
- VK_KHR_sampler_ycbcr_conversion
- VK_KHR_shader_draw_parameters
- VK_KHR_shader_float16_int8
- VK_KHR_storage_buffer_storage_class
- VK_KHR_swapchain
- VK_KHR_uniform_buffer_standard_layout
- VK_KHR_variable_pointers

**ARM-specific extensions (may be available):**
- VK_ARM_* — Need device query to confirm
- VK_KHR_performance_query

### Mali-G57 Valhall Architecture Notes

- **Warp size**: 4 quads = 16 threads
- **Shader core**: Each of the 2 cores has its own execution engine
- **Workgroup sizing**: Optimal workgroup size is typically a multiple of 64 (4 warps)
- **Shared memory**: 16 KB per core typical for Valhall
- **Max compute workgroup invocations**: Likely 256 or 512 per the Vulkan 1.1 spec

### Known Mali-G57 Limitations
- No native fp64 (fp64 emulated)
- No VK_KHR_ray_tracing (Valhall v1 lacks HW RT)
- Limited to Vulkan 1.1 (no Vulkan 1.2+) on this driver
- No VK_KHR_timeline_semaphore in older drivers

## Vulkaninfo Status

`vulkaninfo` binary is **not available** on the device. Options for Phase 3:
1. Cross-compile vulkaninfo from Vulkan SDK for ARM64 and push via ADB
2. Run a minimal Vulkan capability probe via NDK C++ test app
3. Use `dumpsys vulkan` (returned empty on this device)

## Performance Characteristics (Observed)

From `dumpsys gfxinfo` (TikTok session):
- 50th percentile frame time: 16ms (~60 FPS)
- 90th percentile: 26ms (~38 FPS)
- 99th percentile: 36ms (~28 FPS)
- Janky frames: 4.09% (legacy: 71.71%)
- GPU 50th percentile: 5ms
- GPU 90th percentile: 11ms
- Pipeline: Skia (OpenGL)

Note: GPU composition is already active. UI currently renders via OpenGL (Skia), not Vulkan.
