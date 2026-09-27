package com.github.warren_bank.mock_location.ui.logic;

import com.github.warren_bank.mock_location.ui.interfaces.RuntimePermissionsListener;

public final class RuntimeStartRouter {
    private RuntimeStartRouter() {}

    public static RuntimePermissionsListener choose(
        RuntimePermissionsListener explicitRequester,
        RuntimePermissionsListener currentActivity
    ) {
        return (explicitRequester != null) ? explicitRequester : currentActivity;
    }
}
