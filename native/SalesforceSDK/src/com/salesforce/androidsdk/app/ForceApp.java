/*
 * Copyright (c) 2012, salesforce.com, inc.
 * All rights reserved.
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * - Neither the name of salesforce.com, inc. nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission of salesforce.com, inc.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.salesforce.androidsdk.app;

import java.net.URI;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import com.salesforce.androidsdk.auth.AccountWatcher;
import com.salesforce.androidsdk.auth.AccountWatcher.AccountRemoved;
import com.salesforce.androidsdk.auth.AuthenticatorService;
import com.salesforce.androidsdk.auth.HttpAccess;
import com.salesforce.androidsdk.auth.LoginServerManager;
import com.salesforce.androidsdk.auth.OAuth2;
import com.salesforce.androidsdk.phonegap.BootConfig;
import com.salesforce.androidsdk.rest.ClientManager;
import com.salesforce.androidsdk.rest.ClientManager.LoginOptions;
import com.salesforce.androidsdk.security.Encryptor;
import com.salesforce.androidsdk.security.PasscodeManager;
import com.salesforce.androidsdk.ui.LoginActivity;
import com.salesforce.androidsdk.ui.SalesforceDroidGapActivity;
import com.salesforce.androidsdk.ui.SalesforceR;
import com.salesforce.androidsdk.util.EventsObservable;
import com.salesforce.androidsdk.util.EventsObservable.EventType;

/**
 * Singleton class used as an interface to the various
 * functions provided by the Salesforce SDK. This class
 * should be instantiated in order to use the Salesforce SDK.
 */
public class ForceApp implements AccountRemoved {

    /**
     * Current version of this SDK.
     */
    public static final String SDK_VERSION = "2.0.0unstable";

    /**
     * Last phone version.
     */
    private static final int GINGERBREAD_MR1 = 10;

    /**
     * Default app name.
     */
    private static final String DEFAULT_APP_DISPLAY_NAME = "Salesforce";

    /**
     * Instance of the ForceApp to use for this process.
     */
    protected static ForceApp INSTANCE;

    protected Context context;
    protected String appEncryptionKey;
    protected LoginOptions loginOptions;
    private String encryptionKey;
    private AccountWatcher accWatcher;
    private SalesforceR salesforceR = new SalesforceR();
    private PasscodeManager passcodeManager;
    private LoginServerManager loginServerManager;

    /**
     * Returns a singleton instance of this class.
     *
     * @param context Application context.
     * @return Singleton instance of ForceApp.
     */
    public static ForceApp getInstance() {
    	if (INSTANCE != null) {
    		return INSTANCE;
    	} else {
            throw new RuntimeException("Applications need to call ForceApp.init() first.");
    	}
    }

    /**
     * Protected constructor.
     *
     * @param context Application context.
     * @param key Key used for encryption - must be Base64 encoded.
     *
     * 			  {@link Encryptor#isBase64Encoded(String)} can be used to
     * 		      determine whether the generated key is Base64 encoded.
     *
     * 		      {@link Encryptor#hash(String, String)} can be used to
     * 		      generate a Base64 encoded string.
     *
     * 		      For example:
     * 			  <code>
     * 			  Encryptor.hash(name + "12s9adfgret=6235inkasd=012", name + "12kl0dsakj4-cuygsdf625wkjasdol8");
     * 			  </code>
     *
     * @param loginOptions Login options used - must be non null for a native app, can be null for a hybrid app.
     */
    protected ForceApp(Context context, String key, LoginOptions loginOptions) {
    	this.context = context;
    	this.appEncryptionKey = key;
    	this.loginOptions = loginOptions;
    }

    /**
     * @return The class for the main activity.
     */
    public Class<? extends Activity> getMainActivityClass() {
    	return null;
    }

    /**
     * This function must return the same value for name
     * even when the application is restarted. The value this
     * function returns must be Base64 encoded.
     *
     * {@link Encryptor#isBase64Encoded(String)} can be used to
     * determine whether the generated key is Base64 encoded.
     *
     * {@link Encryptor#hash(String, String)} can be used to
     * generate a Base64 encoded string.
     *
     * For example:
     * <code>
     * Encryptor.hash(name + "12s9adfgret=6235inkasd=012", name + "12kl0dsakj4-cuygsdf625wkjasdol8");
     * </code>
     *
     * @param name The name associated with the key.
     * @return The key used for encrypting salts and keys.
     */
    public String getKey(String name) {
    	return null;
    }

    /**
     * Before 1.3, SalesforceSDK was packaged as a jar, and project had to provide a subclass of SalesforceR.
     * Since 1.3, SalesforceSDK is packaged as a library project and we no longer need to to that.
     * @return SalesforceR object which allows reference to resources living outside the SDK.
     */
    public SalesforceR getSalesforceR() {
        return salesforceR;
    }

    /**
     * @return the class of the activity used to perform the login process and create the account.
     * You can override this if you want to customize the LoginAcitivty
     */
    public Class<? extends Activity> getLoginActivityClass() {
        return LoginActivity.class;
    }

	/**
     * Returns login options associated with the app.
     *
	 * @return LoginOptions instance.
	 */
	public LoginOptions getLoginOptions() {
		if (isHybrid()) {
            final BootConfig config = BootConfig.getBootConfig(context);

            // Get clientManager
            final LoginOptions loginOptions = new LoginOptions(null, // set by app
                                                         getPasscodeHash(),
                                                         config.getOauthRedirectURI(),
                                                         config.getRemoteAccessConsumerKey(),
                                                         config.getOauthScopes());
        
            return loginOptions;
		} else if (loginOptions != null) {
			return loginOptions;
		} else {
            throw new RuntimeException("Native applications need to pass in a valid set of login options while initializing ForceApp.");
		}
	}

	/**
	 * Initializes components required for this class
	 * to properly function. This method should be called
	 * by apps using the Salesforce Mobile SDK.
	 *
	 * @param context Application context.
     * @param key Key used for encryption.
     * @param loginOptions Login options used - must be non null for a native app, can be null for a hybrid app.
	 */
    public static void init(Context context, String key, LoginOptions loginOptions) {
    	if (INSTANCE == null) {
    		INSTANCE = new ForceApp(context, key, loginOptions);
    	}

        // Initializes the encryption module.
        Encryptor.init(context);

        // Initializes the HTTP client.
        HttpAccess.init(context, INSTANCE.getUserAgent());

        // Ensures that we have a CookieSyncManager instance.
        CookieSyncManager.createInstance(context);

        // Initializes an AccountWatcher instance.
        INSTANCE.accWatcher = new AccountWatcher(context, INSTANCE);

        // Upgrades to the latest version.
        UpgradeManager.getInstance().upgradeAccMgr();
        EventsObservable.get().notifyEvent(EventType.AppCreateComplete);
    }

    /**
     * Returns whether the SDK should automatically logout when the
     * access token is revoked. This should be overridden to return
     * false, if the app wants to handle cleanup by itself when the
     * access token is revoked.
     *
     * @return True - if the SDK should automatically logout, False - otherwise.
     */
    public boolean shouldLogoutWhenTokenRevoked() {
    	return true;
    }

    /**
     * Returns the application context.
     *
     * @return Application context.
     */
    public Context getAppContext() {
    	return context;
    }

    @Override
    public void onAccountRemoved() {
        INSTANCE.cleanUp(null);
    }

    /**
     * Returns the login server manager associated with ForceApp.
     *
     * @return LoginServerManager instance.
     */
    public synchronized LoginServerManager getLoginServerManager() {
        if (loginServerManager == null) {
        	loginServerManager = new LoginServerManager(context);
        }
        return loginServerManager;
    }    

    /**
     * Returns the passcode manager associated with ForceApp.
     *
     * @return PasscodeManager instance.
     */
    public synchronized PasscodeManager getPasscodeManager() {
        if (passcodeManager == null) {
            passcodeManager = new PasscodeManager(context);
        }
        return passcodeManager;
    }

    /**
     * Changes the passcode to a new value.
     *
     * @param oldPass Old passcode.
     * @param newPass New passcode.
     */
    public synchronized void changePasscode(String oldPass, String newPass) {
        if (!isNewPasscode(oldPass, newPass)) {
            return;
        }

        // Resets the cached encryption key, since the passcode has changed.
        encryptionKey = null;
        ClientManager.changePasscode(oldPass, newPass);
    }

    /**
     * Checks if the new passcode is different from the old passcode.
     *
     * @param oldPass Old passcode.
     * @param newPass New passcode.
     * @return True - if the new passcode is different from the old passcode, False - otherwise.
     */
    protected boolean isNewPasscode(String oldPass, String newPass) {
        return ((oldPass == null && newPass == null)
                || (oldPass != null && newPass != null && oldPass.trim().equals(newPass.trim())));
    }

    /**
     * Returns the encryption key being used.
     *
     * @param actualPass Passcode.
     * @return Encryption key for passcode.
     */
    public synchronized String getEncryptionKeyForPasscode(String actualPass) {
        if (actualPass != null && !actualPass.trim().equals("")) {
            return actualPass;
        }
        if (encryptionKey == null) {
            encryptionKey = getPasscodeManager().hashForEncryption("");
        }
        return encryptionKey;
    }

    /**
     * Returns the app display name used by the passcode dialog.
     *
     * @return App display string.
     */
    public String getAppDisplayString() {
    	return DEFAULT_APP_DISPLAY_NAME;
    }

    /**
     * Returns the passcode hash being used.
     *
     * @return The hashed passcode, or null if it's not required.
     */
    public String getPasscodeHash() {
        return passcodeManager == null ? null : passcodeManager.getPasscodeHash();
    }

    /**
     * Returns the name of the application (as defined in AndroidManifest.xml).
     *
     * @return The name of the application.
     */
    public String getApplicationName() {
        return context.getPackageManager().getApplicationLabel(context.getApplicationInfo()).toString();
    }

    /**
     * Checks if network connectivity exists.
     *
     * @return True - if network is available, False - otherwise.
     */
    public boolean hasNetwork() {
    	return HttpAccess.DEFAULT.hasNetwork();
    }

    /**
     * Cleans up cached credentials and data.
     *
     * @param frontActivity Front activity.
     */
    protected void cleanUp(Activity frontActivity) {

        // Finishes front activity if specified.
        if (frontActivity != null) {
            frontActivity.finish();
        }

        // Resets passcode and encryption key, if any.
        getPasscodeManager().reset(context);
        passcodeManager = null;
        encryptionKey = null;
        UUIDManager.resetUuids();
    }

    /**
     * Starts login flow if user account has been removed.
     */
    protected void startLoginPage() {

        // Clears cookies.
        CookieSyncManager.createInstance(context);
        CookieManager.getInstance().removeAllCookie();

        // Restarts the application.
        final Intent i = new Intent(context, getMainActivityClass());
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }

    /**
     * Wipes out the stored authentication credentials (removes the account)
     * and restarts the app, if specified.
     *
     * @param frontActivity Front activity.
     */
    public void logout(Activity frontActivity) {
        logout(frontActivity, true);
    }

    /**
     * Wipes out the stored authentication credentials (removes the account)
     * and restarts the app, if specified.
     *
     * @param frontActivity Front activity.
     * @param showLoginPage True - if the login page should be shown, False - otherwise.
     */
    public void logout(Activity frontActivity, final boolean showLoginPage) {
        final ClientManager clientMgr = new ClientManager(context, getAccountType(), null, shouldLogoutWhenTokenRevoked());
		final AccountManager mgr = AccountManager.get(context);
        final String refreshToken = ForceApp.decryptWithPasscode(mgr.getPassword(clientMgr.getAccount()), getPasscodeHash());
        final String clientId = ForceApp.decryptWithPasscode(mgr.getUserData(clientMgr.getAccount(), AuthenticatorService.KEY_CLIENT_ID), getPasscodeHash());
        final String loginServer = ForceApp.decryptWithPasscode(mgr.getUserData(clientMgr.getAccount(), AuthenticatorService.KEY_INSTANCE_URL), getPasscodeHash());
        if (accWatcher != null) {
    		accWatcher.remove();
    		accWatcher = null;
    	}
    	cleanUp(frontActivity);

    	// Removes the exisiting account, if any.
    	if (clientMgr.getAccount() == null) {
    		EventsObservable.get().notifyEvent(EventType.LogoutComplete);
    		if (showLoginPage) {
    			startLoginPage();
    		}
    	} else {
    		clientMgr.removeAccountAsync(new AccountManagerCallback<Boolean>() {

    			@Override
    			public void run(AccountManagerFuture<Boolean> arg0) {
    				EventsObservable.get().notifyEvent(EventType.LogoutComplete);
    				if (showLoginPage) {
    					startLoginPage();
    				}
    			}
    		});
    	}

    	// Revokes the existing refresh token.
        if (shouldLogoutWhenTokenRevoked()) {
        	new RevokeTokenTask(refreshToken, clientId, loginServer).execute();
        }
    }

    /**
     * Returns a user agent string based on the mobile SDK version. We are building
     * a user agent of the form:
     *   SalesforceMobileSDK/<salesforceSDK version> android/<android OS version> appName/appVersion <Native|Hybrid>
     *
     * @return The user agent string to use for all requests.
     */
    public final String getUserAgent() {
        String appName = "";
        String appVersion = "";
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            appName = context.getString(packageInfo.applicationInfo.labelRes);
            appVersion = packageInfo.versionName;
        } catch (NameNotFoundException e) {
            Log.w("ForceApp:getUserAgent", e);
        }
	    String nativeOrHybrid = (isHybrid() ? "Hybrid" : "Native");
	    return String.format("SalesforceMobileSDK/%s android mobile/%s (%s) %s/%s %s",
	            SDK_VERSION, Build.VERSION.RELEASE, Build.MODEL, appName, appVersion, nativeOrHybrid);
	}

	/**
	 * Returns whether the application is a hybrid application or not.
	 *
	 * @return True - if this is an hybrid application, False - otherwise.
	 */
	public boolean isHybrid() {
		return SalesforceDroidGapActivity.class.isAssignableFrom(getMainActivityClass());
	}

    /**
     * Returns the authentication account type (should match authenticator.xml).
     *
     * @return Account type string.
     */
    public String getAccountType() {
        return context.getString(getSalesforceR().stringAccountType());
    }

    /**
     * Returns whether the app is running on a tablet or not.
     *
     * @return True - if application is running on a tablet, False - otherwise.
     */
    public static boolean isTablet() {
        if (Build.VERSION.SDK_INT <= GINGERBREAD_MR1) {
            return false;
        } else if ((INSTANCE.context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(this.getClass()).append(": {\n")
          .append("   accountType: ").append(getAccountType()).append("\n")
          .append("   userAgent: ").append(getUserAgent()).append("\n")
          .append("   mainActivityClass: ").append(getMainActivityClass()).append("\n")
          .append("   isFileSystemEncrypted: ").append(Encryptor.isFileSystemEncrypted()).append("\n");
        if (passcodeManager != null) {

            // passcodeManager may be null at startup if the app is running in debug mode.
            sb.append("   hasStoredPasscode: ").append(passcodeManager.hasStoredPasscode(context)).append("\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Encrypts the data using the passcode as the encryption key.
     *
     * @param data Data to be encrypted.
     * @param passcode Encryption key.
     * @return Encrypted data.
     */
    public static String encryptWithPasscode(String data, String passcode) {
        return Encryptor.encrypt(data, ForceApp.INSTANCE.getEncryptionKeyForPasscode(passcode));
    }

    /**
     * Decrypts the data using the passcode as the decryption key.
     *
     * @param data Data to be decrypted.
     * @param passcode Decryption key.
     * @return Decrypted data.
     */
    public static String decryptWithPasscode(String data, String passcode) {
        return Encryptor.decrypt(data, ForceApp.INSTANCE.getEncryptionKeyForPasscode(passcode));
    }

    /**
     * Simple AsyncTask to handle revocation of refresh token upon logout.
     *
     * @author bhariharan
     */
    private class RevokeTokenTask extends AsyncTask<Void, Void, Void> {

    	private String refreshToken;
    	private String clientId;
    	private String loginServer;

    	public RevokeTokenTask(String refreshToken, String clientId, String loginServer) {
    		this.refreshToken = refreshToken;
    		this.clientId = clientId;
    		this.loginServer = loginServer;
    	}

		@Override
		protected Void doInBackground(Void... nothings) {
	        try {
	        	OAuth2.revokeRefreshToken(HttpAccess.DEFAULT, new URI(loginServer), clientId, refreshToken);
	        } catch (Exception e) {
	        	Log.w("ForceApp:revokeToken", e);
	        }
	        return null;
		}
    }
}
