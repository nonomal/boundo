/*
 * Copyright 2021 Clifford Liu
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

package com.madness.collision.util

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.RadioGroup
import androidx.core.content.res.use
import androidx.core.view.get
import coil.imageLoader
import coil.request.ImageRequest
import coil.target.ViewTarget
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.radiobutton.MaterialRadioButton
import com.madness.collision.R
import kotlin.math.roundToInt

object PopupUtil {

    fun selectSingle(context: Context, titleId: Int, entries: TypedArray, index: Int,
                             onConfirmedListener: (pop: CollisionDialog, radioGroup: RadioGroup, checkedIndex: Int) -> Unit)
            = CollisionDialog(context, R.string.text_cancel, R.string.text_OK, false).apply {
        setCustomContent(R.layout.pref_list)
        setTitleCollision(titleId, 0, 0)
        setContent(0)
        val radioGroup: RadioGroup = findViewById(R.id.prefListGroup)
        var checkedIndex = index
        val checkedChangeListener: (CompoundButton, Boolean) -> Unit = { checkBox, isChecked ->
            val i = checkBox.tag as Int
            if (isChecked) {
                checkedIndex = i
            }
        }
        // inflate list
        entries.use {
            for (i in 0 until it.length()) {
                layoutInflater.inflate(R.layout.pref_list_item, radioGroup)
                val item = radioGroup[i] as MaterialRadioButton
                if (i == 0) item.alterMargin(top = 0)
                item.id = R.id.prefListGroup + i + 1
                item.text = it.getString(i)
                item.tag = i
                item.isChecked = index == i
                item.setOnCheckedChangeListener(checkedChangeListener)
            }
        }
        setListener({ dismiss() }, {
            onConfirmedListener.invoke(this, radioGroup, checkedIndex)
        })
    }

    fun selectMulti(context: Context, titleId: Int, entries: TypedArray, icons: List<Any?>, indexes: Set<Int>,
                     onConfirmedListener: (pop: CollisionDialog, container: ViewGroup, checkedIndexes: Set<Int>) -> Unit)
            = CollisionDialog(context, R.string.text_cancel, R.string.text_OK, false).apply {
        setCustomContent(R.layout.popup_select_multi)
        setTitleCollision(titleId, 0, 0)
        setContent(0)
        val radioGroup: ViewGroup = findViewById(R.id.popupSelectMultiContainer)
        val checkedIndexes = indexes.toMutableSet()
        val checkedChangeListener: (CompoundButton, Boolean) -> Unit = { checkBox, isChecked ->
            val checkedIndex = checkBox.tag as Int
            if (isChecked) checkedIndexes.add(checkedIndex)
            else checkedIndexes.remove(checkedIndex)
        }
        // inflate list
        val iconBound = if (icons.isNotEmpty()) X.size(context, 20f, X.DP).roundToInt() else 0
        val iconPadding = if (icons.isNotEmpty()) X.size(context, 6f, X.DP).roundToInt() else 0
        entries.use {
            for (i in 0 until it.length()) {
                layoutInflater.inflate(R.layout.popup_select_multi_item, radioGroup)
                val item = radioGroup[i] as MaterialCheckBox
                if (i == 0) item.alterMargin(top = 0)
                item.id = R.id.popupSelectMultiContainer + i + 1
                item.text = it.getString(i)
                if (icons.isNotEmpty() && icons[i] != null) {
                    item.compoundDrawablePadding = iconPadding
                    val req = ImageRequest.Builder(context).target(object : ViewTarget<MaterialCheckBox> {
                        override val view: MaterialCheckBox = item
                        override fun onStart(placeholder: Drawable?) = setDrawable(placeholder)
                        override fun onSuccess(result: Drawable) = setDrawable(result)
                        override fun onError(error: Drawable?) = setDrawable(error)
                        private fun setDrawable(drawable: Drawable?) {
                            drawable?.setBounds(0, 0, iconBound, iconBound)
                            item.setCompoundDrawablesRelative(drawable, null, null, null)
                        }
                    }).data(icons[i]).build()
                    context.imageLoader.enqueue(req)
                }
                item.tag = i
                item.isChecked = i in indexes
                item.setOnCheckedChangeListener(checkedChangeListener)
            }
        }
        setListener({ dismiss() }, {
            onConfirmedListener.invoke(this, radioGroup, checkedIndexes)
        })
    }
}