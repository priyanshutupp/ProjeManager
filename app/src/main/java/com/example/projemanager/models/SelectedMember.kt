package com.example.projemanager.models

import android.os.Parcel
import android.os.Parcelable

data class SelectedMember(
    val id: String = "",
    val image: String = ""
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(image)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<SelectedMember> {
        override fun createFromParcel(parcel: Parcel): SelectedMember {
            return SelectedMember(parcel)
        }

        override fun newArray(size: Int): Array<SelectedMember?> {
            return arrayOfNulls(size)
        }
    }
}
