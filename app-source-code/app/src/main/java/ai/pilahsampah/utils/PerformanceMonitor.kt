package ai.pilahsampah.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.util.Log
import ai.pilahsampah.BuildConfig

/**
 * Performance monitoring utility for tracking app performance metrics
 */
object PerformanceMonitor {
    private const val TAG = "PerformanceMonitor"
    
    /**
     * Monitors memory usage and logs warnings if memory usage is high
     */
    fun checkMemoryUsage(context: Context) {
        if (!BuildConfig.DEBUG) return
        
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val availableMemory = maxMemory - usedMemory
        
        val memoryInfo = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(memoryInfo)
        
        val usedMemoryMB = usedMemory / (1024 * 1024)
        val maxMemoryMB = maxMemory / (1024 * 1024)
        val availableMemoryMB = availableMemory / (1024 * 1024)
        
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Memory Usage: ${usedMemoryMB}MB / ${maxMemoryMB}MB (Available: ${availableMemoryMB}MB)")
        }
        
        // Warn if memory usage is above 80%
        if (usedMemory > maxMemory * 0.8) {
            Log.w(TAG, "High memory usage detected: ${usedMemoryMB}MB / ${maxMemoryMB}MB")
        }
        
        // Warn if system memory is low
        if (memoryInfo.lowMemory) {
            Log.w(TAG, "System memory is low")
        }
    }
    
    /**
     * Gets current heap allocation information
     */
    fun getHeapInfo(): String {
        val memInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(memInfo)
        
        return "Heap: ${memInfo.dalvikPrivateDirty}KB private, ${memInfo.dalvikSharedDirty}KB shared"
    }
    
    /**
     * Monitors frame rendering performance
     */
    fun logFrameMetrics(frameTimeMs: Long, inferenceTimeMs: Long) {
        if (!BuildConfig.DEBUG) return
        
        if (frameTimeMs > 33) { // Above 30 FPS
            Log.w(TAG, "Slow frame detected: ${frameTimeMs}ms (inference: ${inferenceTimeMs}ms)")
        }
        
        if (inferenceTimeMs > 100) {
            Log.w(TAG, "Slow inference detected: ${inferenceTimeMs}ms")
        }
    }
    
    /**
     * Checks if device has sufficient resources for optimal performance
     */
    fun checkDeviceCapability(context: Context): DeviceCapability {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryClass = activityManager.memoryClass
        val isLowRamDevice = activityManager.isLowRamDevice
        
        return when {
            isLowRamDevice || memoryClass < 128 -> DeviceCapability.LOW
            memoryClass < 256 -> DeviceCapability.MEDIUM
            else -> DeviceCapability.HIGH
        }
    }
    
    enum class DeviceCapability {
        LOW,    // Reduce quality, lower frame rate
        MEDIUM, // Standard settings
        HIGH    // Full quality, high frame rate
    }
}