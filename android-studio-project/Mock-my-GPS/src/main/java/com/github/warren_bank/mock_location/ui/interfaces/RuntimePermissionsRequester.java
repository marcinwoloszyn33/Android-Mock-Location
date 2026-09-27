package com.github.warren_bank.mock_location.ui.interfaces;

public interface RuntimePermissionsRequester {
    public void requestRuntimePermissions();
    public void requestRuntimePermissions(RuntimePermissionsListener listener);
}
