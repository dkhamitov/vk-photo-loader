package d.kh

import com.vk.api.sdk.client.VkApiClient
import com.vk.api.sdk.client.actors.UserActor
import com.vk.api.sdk.httpclient.HttpTransportClient
import com.vk.api.sdk.objects.photos.PhotoAlbumFull
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import java.io.File
import java.nio.file.Paths

/**
 * The code has been written using
 * https://vk.com/dev/methods
 * https://vk.com/dev/photos.get
 * https://vk.com/dev/Java_SDK
 * https://vk.com/dev/permissions
 *
 */
fun main(args: Array<String>) {
    val appId = System.getProperty("appId").toInt()
    val token = System.getProperty("token")
    val userId = readInt("Enter the userId please")

    val vk = VkApiClient(HttpTransportClient.getInstance())
    val actor = UserActor(appId, token)

    val albums = vk.photos()
            .getAlbums(actor)
            .needSystem(true)
            .ownerId(userId)
            .execute()
            .items
    val albumTitleById = albums.associateBy({ it.id.toString() }, { it.title })

    printAlbums(albums)

    val albumToLoad = readString("Which album to download?")
    val albumFolder = readString("Where to download the album?")
    val photos = vk.photos()
            .get(actor)
            .ownerId(userId)
            .albumId(albumToLoad)
            .photoIds()
            .photoSizes(true)
            .execute()
            .items

    val urls = photos.map { it.sizes }
            .map { it.maxBy { it.type } }
            .map { it!!.src }

    savePhotos(urls, albumFolder, albumTitleById[albumToLoad]!!)
}

private fun printAlbums(albums: Collection<PhotoAlbumFull>) {
    val titleIndent = albums.map({ it.title.trim().length }).max()
    val idIndent = albums.map({ it.id.toString().length }).max()
    println("Albums")
    albums.forEach { println("%-${titleIndent}s: %${idIndent}s: %s".format(it.title.trim(), it.id, it.size)) }
}

private fun savePhotos(urls: List<String>, root: String, album: String) {
    val folder = Paths.get(root, album).toFile()
    folder.mkdirs()
    val httpClient = HttpClients.createDefault()
    urls.forEachIndexed { i, url ->
        val prefix = i + 1
        val file = File(folder, "$prefix-${url.substringAfterLast("/")}")
        httpClient.execute(HttpGet(url)).entity.writeTo(file.outputStream())
        println("$prefix downloaded")
    }
}