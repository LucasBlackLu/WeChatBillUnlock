package com.wxbill.unlock

import android.app.Application
import android.content.Context
import com.wxbill.unlock.core.OpenBill

class App : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        Link.start()
        OpenBill.start(this)
    }
}
