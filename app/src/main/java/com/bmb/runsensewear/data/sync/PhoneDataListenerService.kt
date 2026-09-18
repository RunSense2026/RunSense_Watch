package com.bmb.runsensewear.data.sync

import com.bmb.runsensewear.RunSenseApp
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

/**
 * 폰 앱이 보내는 Data Layer 이벤트를 앱이 꺼져 있어도 받습니다.
 * 매니페스트의 intent-filter가 `/runsense` 접두사만 이 서비스로 보내 줍니다.
 */
class PhoneDataListenerService : WearableListenerService() {

    private val sync: PhoneSyncManager
        get() = (application as RunSenseApp).container.phoneSyncManager

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        sync.handleDataEvents(dataEvents)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        sync.handleMessage(messageEvent)
    }
}
