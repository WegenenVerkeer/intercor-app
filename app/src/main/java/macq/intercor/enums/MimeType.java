package macq.intercor.enums;

import android.support.annotation.NonNull;

public enum MimeType {
    G_SPREADSHEET("application/vnd.google-apps.spreadsheet"),
    G_PRESENTATION("application/vnd.google-apps.presentation"),
    G_FOLDER("application/vnd.google-apps.folder"),
    G_FILE("application/vnd.google-apps.file"),
    G_DOCUMENT("application/vnd.google-apps.document");

    private String name;

    MimeType(String name) {
        this.name = name;
    }

    @Override
    @NonNull
    public String toString() {
        return name;
    }
}
