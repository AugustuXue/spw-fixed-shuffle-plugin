# SPW Fixed Shuffle Queue Plugin

Fixed Shuffle Queue 鏄竴娆句负 [Salt Player for Windows (SPW)](https://github.com/Moriafly/SaltPlayerForWindows) 寮€鍙戠殑鎻掍欢銆

## 鍔熻兘浠嬬粛
鏈彃浠舵棬鍦ㄤ紭鍖栧拰鈥滃姭鎸佲€濆師鐢熺殑闅忔満鎾斁浣撻獙銆傚綋浣﹀湪 SPW 涓荤晫闈㈢偣鍑汇€愰殢鏈烘挱鏀俱€戞椂锛氥€
- 鎻掍欢浼氱灛闂存嫤鎴鎿嶄綔锛屽皢褰撳墠鐨勬甯告挱鏀鹃槦鍒楄繘琛**瀹屽叏娲楃墝**鍜岄噸缁勩€
- 灏嗘礂鐗屽悗鐨勫浐瀹氶『搴忛噸鏂板啓鍏ユ挱鏀鹃槦鍒楋紝骞朵粠绗竴棣栧紑濮嬫挱鏀俱€
- 鑷姩灏嗘挱鏀炬ā寮忓垏鎹㈠洖銆愬垪琛ㄥ惊鐜€戯紝璁╀綘鍙互鍦ㄦ挱鏀鹃槦鍒 UI 涓洿鎺ョ湅鍒板畬鏁寸殑闅忔満鍚庨『搴忥紝涓斿彲浠ョ户缁娇鐢ㄥ師鐢熺殑鎷栨嫿鎺掑簭鍔熻兘銆

**鏃㈤渶瑕佷换浣曡缃紝鐣岄潰鏃犳劅浜や簰锛**

## 鏋勫缓鏂规硶
鏈」鐩凡鑴辩 spw-workshop-api 妯℃澘鐙珴鍑烘潵锛屽彲鐩存帴浣跨敤 Gradle 鏋勫缓銆

**鐜瑕佹眰**锛氭湰鏈哄繀椤诲畨瑁 JDK 21銆

`ash
# 鎸囧畾 JDK 21 骞舵墽琛屾彃浠舵墦鍖呬换鍔
./gradlew plugin
`

缂栬瘧瀹屾垚鍚庯紝鎻掍欢 .zip 鍖呬細鑷姩澶嶅埗鍒 %APPDATA%\Salt Player for Windows\workshop\plugins\ 鐩綍涓嬨€

## 鎶€鏈師鐞
鐢间簬 SPW 鐩墠鐨 Workshop API 灏氭湭鏆撮湶鎾斁闃熷垪鐨勬搷浣滄帴鍙ｏ紝鏈彃浠堕噰鐢ㄤ簡 **鍙嶅皠** 鐨勬柟寮忥細
1. 鍚姩鍚庡彴绾跨▼锛岃疆璇㈤敊鎹 PlaybackQueueState (hunxiao ndroidx.compose.ui.ne) 鐨 StateFlow銆
2. 瑙ｆ瀽出混娣嗙殑闃熷垪灞炴€ (Ԩ(), Ϳ()) 和底层的 PiscesMediaItem銆
3. 閫氳繃反射 PlaybackController.INSTANCE.setPlaybackQueue() 进行队列替换和模式切换。

## 致谢
感谢 SPW 开发者提供的强大播放器及 Workshop API。
