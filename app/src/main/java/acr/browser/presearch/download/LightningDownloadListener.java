/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.presearch.download;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.text.format.Formatter;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;

import com.anthonycr.grant.PermissionsManager;
import com.anthonycr.grant.PermissionsResultAction;

import javax.inject.Inject;

import acr.browser.presearch.R;
import acr.browser.presearch.database.downloads.DownloadsRepository;
import acr.browser.presearch.di.Injector;
import acr.browser.presearch.dialog.BrowserDialog;
import acr.browser.presearch.log.Logger;
import acr.browser.presearch.preference.UserPreferences;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

public class LightningDownloadListener implements DownloadListener {

    private static final String TAG = "LightningDownloader";

    private final Activity mActivity;

    @Inject UserPreferences userPreferences;
    @Inject DownloadHandler downloadHandler;
    @Inject DownloadsRepository downloadsRepository;
    @Inject Logger logger;

    public LightningDownloadListener(Activity context) {
        Injector.getInjector(context).inject(this);
        mActivity = context;
    }

    @Override
    public void onDownloadStart(@NonNull final String url, final String userAgent,
                                final String contentDisposition, final String mimetype, final long contentLength) {
        PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(mActivity,
            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
            new PermissionsResultAction() {
                @Override
                public void onGranted() {
                    final String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
                    final String downloadSize;

                    if (contentLength > 0) {
                        downloadSize = Formatter.formatFileSize(mActivity, contentLength);
                    } else {
                        downloadSize = mActivity.getString(R.string.unknown_size);
                    }

                    DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                downloadHandler.onDownloadStart(mActivity, userPreferences, url, userAgent, contentDisposition, mimetype, downloadSize);
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                break;
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(mActivity); // dialog
                    String message = mActivity.getString(R.string.dialog_download, downloadSize);
                    Dialog dialog = builder.setTitle(fileName)
                        .setMessage(message)
                        .setPositiveButton(mActivity.getResources().getString(R.string.action_download),
                            dialogClickListener)
                        .setNegativeButton(mActivity.getResources().getString(R.string.action_cancel),
                            dialogClickListener).show();
                    BrowserDialog.setDialogSize(mActivity, dialog);
                    logger.log(TAG, "Downloading: " + fileName);
                }

                @Override
                public void onDenied(String permission) {
                    //TODO show message
                }
            });
    }
}
