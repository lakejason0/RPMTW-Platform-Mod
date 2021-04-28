package siongsng.rpmtwupdatemod.function;

import net.minecraft.resource.ResourcePackProvider;
import org.apache.commons.io.FileUtils;
import siongsng.rpmtwupdatemod.RpmtwUpdateMod;
import siongsng.rpmtwupdatemod.json;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

public class PackVersionCheck {
    public static Path PackDir = Paths.get(System.getProperty("user.home") + "/.rpmtw/1.16"); //資源包的放置根目錄
    public static Path PackFile = PackDir.resolve("RPMTW-1.16.zip"); //資源包檔案位置
    public String UpdateFile = PackDir + "/Update.txt"; //更新暫存檔案放置位置
    public String Latest_ver = json.ver("https://api.github.com/repos/SiongSng/ResourcePack-Mod-zh_tw/releases/latest").toString(); //取得最新版本Tag
    public String Latest_ver_n = Latest_ver.split("RPMTW-1.16-V")[1]; //取得數字ID

    public PackVersionCheck(Set<ResourcePackProvider> providers) throws IOException {
        if (!Files.isDirectory(PackDir)) {
            Files.createDirectories(PackDir);
        }
        if (!Files.exists(Paths.get(PackDir + "/Update.txt"))) { //如果沒有更新檔案
            Files.createFile(Paths.get(PackDir + "/Update.txt")); //建立更新檔案
            FileWriter.Writer(Latest_ver_n, UpdateFile); //寫入最新版本
        }
        FileReader fr = new FileReader(UpdateFile);
        BufferedReader br = new BufferedReader(fr);
        int Old_ver = 0;
        while (br.ready()) {
            Old_ver = Integer.parseInt(br.readLine());
            System.out.println(br.readLine());
        }
        fr.close();
        RpmtwUpdateMod.LOGGER.info("正在準備檢測資源包版本，最新版本:" + Latest_ver);
        try {
            if (Integer.parseInt(Latest_ver_n) > Old_ver || !Files.exists(PackFile)) {
                RpmtwUpdateMod.LOGGER.info("§6偵測到資源包版本過舊，正在進行更新並重新載入中...。目前版本為:" + Old_ver + "最新版本為:" + Latest_ver_n);
                FileWriter.Writer(Latest_ver_n, UpdateFile); //寫入最新版本
                FileUtils.copyURLToFile(new URL(json.loadJson("https://api.github.com/repos/SiongSng/ResourcePack-Mod-zh_tw/releases/latest").toString()), PackFile.toFile()); //下載資源包檔案
            } else {
                RpmtwUpdateMod.LOGGER.info("目前的RPMTW版本已經是最新的了!!");
            }
            Class.forName("siongsng.rpmtwupdatemod.packs.LoadPack").getMethod("init", Set.class).invoke(null, providers);
        } catch (Exception e) {
            RpmtwUpdateMod.LOGGER.error("發生未知錯誤: " + e);
        }
    }
}
