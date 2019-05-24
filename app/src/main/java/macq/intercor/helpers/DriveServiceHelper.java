package macq.intercor.helpers;

import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.TeamDrive;
import com.google.api.services.drive.model.TeamDriveList;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import macq.intercor.enums.MimeType;

public class DriveServiceHelper {

    private final static String TAG = "DriveServiceHelper";
    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private final Drive mDrive;
    private final String teamDriveID = "0ALnuaR6ZivRiUk9PVA";
    private String teamFolderID;
    private String filename;

    public DriveServiceHelper(Drive driveService, String teamName) {
        mDrive = driveService;
        filename = new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + "_" + teamName;
    }

    public Task<String> createFile() {
        return Tasks.call(mExecutor, () -> {
            List<String> parents = Arrays.asList(teamFolderID);
            File metadata = new File()
                .setParents(parents)
                .setTeamDriveId(teamDriveID)
                .setMimeType(MimeType.G_SPREADSHEET.toString())
                .setName(filename);
            File googleFile = mDrive.files().create(metadata).setSupportsTeamDrives(true).execute();
            if(googleFile == null) throw new IOException("Null result when requesting file creation.");
            return googleFile.getId();
        });
    }

    public Task<String> createFolder(String folderName) {
        return Tasks.call(mExecutor, () -> {
            List<String> parents = Arrays.asList(teamDriveID);
            File metadata = new File()
                .setName(folderName)
                .setMimeType(MimeType.G_FOLDER.toString())
                .setParents(parents);
            File folder = mDrive.files().create(metadata).setSupportsTeamDrives(true).setFields("id").execute();
            return folder.getId();
        });
    }

    public Task<String> getFileID(String filename, List<String> parentFolders) {
        return Tasks.call(mExecutor, () -> {
            StringBuilder query = new StringBuilder();
            query = query.append("name = '").append(filename).append("'");

            for (String parent: parentFolders) {
                query = query.append(" and ").append("'").append(parent).append("' in parents");
            }

            FileList list = mDrive.files().list()
                    .setQ(query.toString())
                    .setCorpora("teamDrive")
                    .setIncludeTeamDriveItems(true)
                    .setTeamDriveId(teamDriveID)
                    .setSupportsTeamDrives(true)
                    .execute();

            return !list.getFiles().isEmpty() ? list.getFiles().get(0).getId() : "";
        });
    }

    public Task<Void> updateFile(String fileId, java.io.File file) {
        return Tasks.call(mExecutor, () -> {
            File metadata = new File()
                    .setName(filename)
                    .setMimeType(MimeType.G_SPREADSHEET.toString());
            FileContent contentStream = new FileContent("text/csv", file);
            Log.i(TAG, "Lenght is: " + contentStream.getLength());
            mDrive.files().update(fileId, metadata, contentStream)
                    .setSupportsTeamDrives(true)
                    .execute();
            return null;
        });
    }

    public String getFilename() {
        return filename;
    }

    public String getTeamFolderID() {
        return teamFolderID;
    }

    public void setTeamFolderID(String teamFolderID) {
        this.teamFolderID = teamFolderID;
    }
}
