package com.jonipharju.less.launcher

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import com.jonipharju.less.launcher.proto.LauncherUserData
import java.io.InputStream
import java.io.OutputStream

internal val Context.launcherUserDataStore: DataStore<LauncherUserData> by dataStore(
    fileName = "launcher_user_data.pb",
    serializer = LauncherUserDataSerializer,
)

private object LauncherUserDataSerializer : Serializer<LauncherUserData> {
    override val defaultValue: LauncherUserData = LauncherUserData.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): LauncherUserData =
        try {
            LauncherUserData.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read launcher user data.", exception)
        }

    override suspend fun writeTo(
        t: LauncherUserData,
        output: OutputStream,
    ) = t.writeTo(output)
}
