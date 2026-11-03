/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.core.model

/**
 * Update check lifecycle states.
 */
enum class UpdateState {
    IDLE,            // Not checked yet
    CHECKING,        // In progress
    UP_TO_DATE,      // Already latest
    NEW_VERSION_AVAILABLE, // Update found
    ERROR            // Check failed
}
