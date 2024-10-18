package network


import  javax.crypto.SecretKey
import  javax.crypto.spec.IvParameterSpec

interface EncryptionInterface {

    fun hashStrSha256(str: String): String
    fun generateAESKey(seed: String): SecretKey
    fun generateIV(seed:String): IvParameterSpec
    fun encryptMessage(plaintext: String, aesKey: SecretKey, aesIv: IvParameterSpec): String
    fun decryptMessage(encryptedText: String, aesKey:SecretKey, aesIv: IvParameterSpec):String

}