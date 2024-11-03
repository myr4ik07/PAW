package com.blogspot.copyraite.PAW.Other;

import android.app.Application;

public class AuthPasswordSave extends Application {
    private String enterPassword;
    private String enterLogin;

    public String getEnterLogin() {
        return enterLogin;
    }

    public void setEnterLogin(String enterLogin) {
        this.enterLogin = enterLogin;
    }

    public String getEnterPassword() {
        return enterPassword;
    }

    public void setEnterPassword(String enterPassword) {
        this.enterPassword = enterPassword;
    }
}
