/*
 * ************************************************************************************************
 * * © 2021 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.             *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.gaia.qtil.data.gestures;

import android.os.Parcelable;

import java.util.Set;

public interface GestureContext extends BitItem, Parcelable {

    Set<Action> getSupportedActions();

}
