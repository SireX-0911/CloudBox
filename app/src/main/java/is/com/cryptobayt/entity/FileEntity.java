package is.com.cryptobayt.entity;

import com.google.firebase.database.Exclude;

/**
 * Created by developer on 06.05.2017.
 */

public class FileEntity {
    @Exclude
    String fileUid;
    String name;
    long size;
    @Exclude
    String realAbsolyutPath;

    public FileEntity() {

    }

    public FileEntity(String fileUid, String name, long size) {
        this.fileUid = fileUid;
        this.name = name;
        this.size = size;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @Exclude
    public String getFileUid() {
        return fileUid;
    }

    @Exclude
    public void setFileUid(String fileUid) {
        this.fileUid = fileUid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Exclude
    public String getRealAbsolyutPath() {
        return realAbsolyutPath;
    }

    @Exclude
    public void setRealAbsolyutPath(String realAbsolyutPath) {
        this.realAbsolyutPath = realAbsolyutPath;
    }
}
