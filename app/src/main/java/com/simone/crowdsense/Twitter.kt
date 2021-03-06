package com.simone.crowdsense

import okhttp3.*
import java.sql.Timestamp
import java.security.SecureRandom
import java.net.URLEncoder
import org.apache.commons.codec.binary.Base64
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.SignatureException
import java.util.Formatter
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.apache.commons.codec.binary.Hex
import java.nio.charset.StandardCharsets
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.util.EntityUtils
import twitter4j.*
import twitter4j.conf.ConfigurationBuilder
import java.io.File

object HmacSha1Signature {
    private val HMAC_SHA1_ALGORITHM = "HmacSHA1"

    private fun toHexString(bytes: ByteArray): String {
        val formatter = Formatter()

        for (b in bytes) {
            formatter.format("%02x", b)
        }

        return formatter.toString()
    }

    @Throws(SignatureException::class, NoSuchAlgorithmException::class, InvalidKeyException::class)
    fun calculateRFC2104HMAC(data: String, key: String): String {
        val signingKey = SecretKeySpec(key.toByteArray(), HMAC_SHA1_ALGORITHM)
        val mac = Mac.getInstance(HMAC_SHA1_ALGORITHM)
        mac.init(signingKey)
        return toHexString(mac.doFinal(data.toByteArray()))
    }

}

fun nonceGenerator(): String {
    val secureRandom = SecureRandom()
    val stringBuilder = StringBuilder()
    for (i in 0..14) {
        stringBuilder.append(secureRandom.nextInt(10))
    }
    return stringBuilder.toString()
}

fun twitterOauth(): Request{
    val timestamp = Timestamp(System.currentTimeMillis())
    val ts = URLEncoder.encode((timestamp.getTime()/1000).toString(), "ASCII")
    val nonce = URLEncoder.encode(nonceGenerator(), "ASCII")

    val baseUrl = "https://api.twitter.com/1.1/search/tweets.json"
    val baseUrlEncode = URLEncoder.encode(baseUrl, "ASCII")

    val tweet_mode = URLEncoder.encode("extended", "ASCII")

    val params = "oauth_consumer_key=lWjUOd4cZMQjqCbKw4PwY7i4u&oauth_nonce=$nonce&oauth_signature_method=HMAC-SHA1&oauth_timestamp=$ts&oauth_token=985771825425780736-LRyqxRacUVoF51x7hW73CfBpkXpGfHd&oauth_version=1.0&q=%23LAM_CROWD18&result_type=recent&tweet_mode=$tweet_mode"
    val paramsEncode = URLEncoder.encode(params, "ASCII")

    val baseParams = "GET&$baseUrlEncode&$paramsEncode"
    val signingKey = URLEncoder.encode("tX6buaz7KFV2GWkvGtMrZccW3GMUXu4VdkxsTbMb1pDD3FCl2t", "ASCII") + "&" + URLEncoder.encode("ln9kzDDEARF2up4B5sYhAYyILAZdnnTQM9zbwQfNrt3bs", "ASCII")

    var hmac = HmacSha1Signature.calculateRFC2104HMAC(baseParams, signingKey)
    val bytes = Hex.decodeHex(hmac.toCharArray())
    val base64 = Base64.encodeBase64(bytes)

    hmac = URLEncoder.encode(base64.toString(StandardCharsets.UTF_8), "ASCII")

    val url = "https://api.twitter.com/1.1/search/tweets.json?q=%23LAM_CROWD18&result_type=recent&tweet_mode=$tweet_mode"
    val request = Request.Builder()
            .header("Authorization", "OAuth oauth_consumer_key=\"lWjUOd4cZMQjqCbKw4PwY7i4u\", oauth_token=\"985771825425780736-LRyqxRacUVoF51x7hW73CfBpkXpGfHd\", oauth_signature_method=\"HMAC-SHA1\", oauth_timestamp=\"$ts\", oauth_nonce=\"$nonce\", oauth_version=\"1.0\", oauth_signature=\"$hmac\"")
            .url(url)
            .build()

    return request
}

fun twitterOauthPost(result : String, id : String, issuer : String){

    val consumer = CommonsHttpOAuthConsumer(
            "XgedeBPLyC48g4uZBwfJsjsWB",
            "kgVwDnry5mQUDbriGaKfuc9vw2Es3QwAWXngI9nl2JVy1UIXDB")

    consumer.setTokenWithSecret("1017016558466555910-7TT68vdgfgnqt3CQqguEZljmlHtUNV", "kZyZERAYkGU7b0bS58VBds2Abb4koBtyylYPtFhz4j9f2")

    val ids = id.toLong()
    val resultEncode = URLEncoder.encode(result, "ASCII")
    val issuerEncode = URLEncoder.encode(issuer, "ASCII")

    val httpPost = HttpPost(
            "https://api.twitter.com/1.1/statuses/update.json?status=%40$issuerEncode%20$resultEncode&in_reply_to_status_id=$ids")

    consumer.sign(httpPost)

    val httpClient = DefaultHttpClient()
    val httpResponse = httpClient.execute(httpPost)

    val statusCode = httpResponse.statusLine.statusCode
    val mexCode = httpResponse.statusLine.reasonPhrase

    println(statusCode)
    println(mexCode)
    println(EntityUtils.toString(httpResponse.entity))
}

fun twitterPostPhoto(photo : File, id : String, issuer: String){

     val conf =  ConfigurationBuilder()
                                        .setOAuthConsumerKey("XgedeBPLyC48g4uZBwfJsjsWB")
                                        .setOAuthConsumerSecret("kgVwDnry5mQUDbriGaKfuc9vw2Es3QwAWXngI9nl2JVy1UIXDB")
                                        .setOAuthAccessToken("1017016558466555910-7TT68vdgfgnqt3CQqguEZljmlHtUNV")
                                        .setOAuthAccessTokenSecret("kZyZERAYkGU7b0bS58VBds2Abb4koBtyylYPtFhz4j9f2").build()

    val twitter = TwitterFactory(conf).instance

    val status = StatusUpdate("@$issuer")
    status.setMedia(photo) // set the image to be uploaded here.
    status.inReplyToStatusId = id.toLong()

    twitter.updateStatus(status)

}


class Statuses(val statuses : Array<Tweets>)

class Tweets(val id: String, val full_text: String, val created_at : String)

class Task(val ID: String?, val issuer: String?, val type: String?, val lat: String?,
           val lon: String?, val radius: String?, val duration: String?, val what: String?)