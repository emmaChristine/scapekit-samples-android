/**
 * Designed and developed by Aidan Follestad (@afollestad)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.afollestad.assent.internal

import android.content.Context
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

internal class Data {

  internal val requestQueue = Queue<PendingRequest>()
  internal var currentPendingRequest: PendingRequest? = null
  internal var permissionFragment: PermissionFragment? = null

  companion object {

    val LOCK = Any()
    var instance: Data? = null

    private const val TAG_ACTIVITY = "[assent_permission_fragment/activity]"
    private const val TAG_FRAGMENT = "[assent_permission_fragment/fragment]"

    fun get(): Data {
      if (instance == null) {
        instance = Data()
      }
      return instance ?: throw IllegalStateException()
    }

    fun ensureFragment(context: Context): PermissionFragment = with(get()) {
      if (context !is FragmentActivity) {
        throw UnsupportedOperationException(
            "Unable to assure the permission Fragment on Context $context"
        )
      }
      permissionFragment = if (permissionFragment == null) {
        PermissionFragment().apply {
          log("Created new PermissionFragment for Context")
          context.transact { add(this@apply, TAG_ACTIVITY) }
        }
      } else {
        log("Re-using PermissionFragment for Context")
        permissionFragment
      }
      return permissionFragment ?: throw IllegalStateException()
    }

    fun ensureFragment(context: Fragment): PermissionFragment = with(get()) {
      permissionFragment = if (permissionFragment == null) {
        PermissionFragment().apply {
          log("Created new PermissionFragment for parent Fragment")
          context.transact { add(this@apply, TAG_FRAGMENT) }
        }
      } else {
        log("Re-using PermissionFragment for parent Fragment")
        permissionFragment
      }
      return permissionFragment ?: throw IllegalStateException()
    }

    fun forgetFragment() = with(get()) {
      log("forgetFragment()")
      permissionFragment?.detach()
      permissionFragment = null
    }
  }
}

fun log(message: String) {
  Log.d("AssentData", message)
}
