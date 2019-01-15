package us.nonda.aidemo

/**
 * Created by yanghe on 2019/1/14.
 * <p>
 */
interface IRecognizer{
    fun create()
    fun startListening()
    fun stopListening()
    fun destroy()
    fun setCallback(callback: IRecognizerCallback)
}