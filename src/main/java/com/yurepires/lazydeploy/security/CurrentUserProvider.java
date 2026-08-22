package com.yurepires.lazydeploy.security;

import java.util.UUID;

public interface CurrentUserProvider {

    UUID getCurrentUserId();
}
