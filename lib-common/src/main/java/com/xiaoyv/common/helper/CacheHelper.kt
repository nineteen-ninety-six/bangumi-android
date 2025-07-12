package com.xiaoyv.common.helper

import android.os.Parcelable
import com.blankj.utilcode.util.CacheDiskUtils
import com.blankj.utilcode.util.EncryptUtils
import com.blankj.utilcode.util.SPUtils
import com.xiaoyv.blueprint.kts.launchProcess
import com.xiaoyv.blueprint.kts.toJson
import com.xiaoyv.common.api.parser.entity.HomeIndexEntity
import com.xiaoyv.common.api.parser.entity.MediaDetailEntity
import com.xiaoyv.common.api.response.GalleryEntity
import com.xiaoyv.common.config.bean.SearchItem
import com.xiaoyv.common.kts.fromJson
import com.xiaoyv.common.kts.parcelableCreator
import com.xiaoyv.widget.kts.subListLimit

/**
 * Class: [CacheHelper]
 *
 * @author why
 * @since 12/10/23
 */
object CacheHelper {
    private const val KEY_CACHE_HISTORY = "cache-history"
    private const val KEY_CACHE_HOME = "cache-home"
    private const val KEY_CACHE_PROCESS = "cache-process"
    private const val KEY_CACHE_BG_IMAGE = "cache-bg-image"

    private val cache by lazy {
        CacheDiskUtils.getInstance()
    }

    /**
     * 保存搜索历史
     */
    fun saveSearchHistory(searchItem: SearchItem) {
        if (searchItem.keyword.isBlank()) return
        launchProcess {
            val key = EncryptUtils.encryptMD5ToString(searchItem.keyword)
            searchItem.timestamp = System.currentTimeMillis()
            SPUtils.getInstance(KEY_CACHE_HISTORY).put(key, searchItem.toJson())
        }
    }

    /**
     * 读取搜索历史
     */
    fun readSearchHistory(): List<SearchItem> {
        return runCatching {
            SPUtils.getInstance(KEY_CACHE_HISTORY).all
                .map { it.value.toString().fromJson<SearchItem>() }
                .filterNotNull()
                .sortedByDescending { it.timestamp }
                .subListLimit(9)
        }.getOrNull().orEmpty()
    }

    /**
     * 清除搜索历史
     */
    fun clearSearchHistory() {
        launchProcess {
            SPUtils.getInstance(KEY_CACHE_HISTORY).clear()
        }
    }

    /**
     * 缓存翻译结果
     */
    fun saveTranslate(cacheKey: String, text: String) {
        cache.put(cacheKey, text)
    }

    /**
     * 读取翻译缓存
     */
    fun readTranslate(cacheKey: String): String {
        return cache.getString(cacheKey).orEmpty()
    }

    /**
     * 空间背景数据图片列表缓存
     */
    var cacheBgImageList: List<GalleryEntity>
        set(value) = cache.put(KEY_CACHE_BG_IMAGE, value.toJson())
        get() = cache.getString(KEY_CACHE_BG_IMAGE).orEmpty()
            .fromJson<List<GalleryEntity>>()
            .orEmpty()

    /**
     * 首页缓存
     */
    var cacheHome: HomeIndexEntity?
        set(value) = cache.put(KEY_CACHE_HOME, value)
        get() = read(KEY_CACHE_HOME)

    /**
     * 进度缓存
     */
    var cacheProcess: List<MediaDetailEntity>
        set(value) = cache.put(KEY_CACHE_PROCESS, value.toJson())
        get() = cache.getString(KEY_CACHE_PROCESS).orEmpty()
            .fromJson<List<MediaDetailEntity>>()
            .orEmpty()

    private inline fun <reified T : Parcelable> read(key: String): T? {
        return runCatching {
            cache.getParcelable(key, parcelableCreator<T>())
        }.onFailure {
            cache.remove(key)
        }.getOrNull()
    }
}