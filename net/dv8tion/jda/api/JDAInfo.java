package net.dv8tion.jda.api;

public class JDAInfo {
   public static final int DISCORD_GATEWAY_VERSION = 6;
   public static final int DISCORD_REST_VERSION = 6;
   public static final int AUDIO_GATEWAY_VERSION = 4;
   public static final String GITHUB = "https://github.com/DV8FromTheWorld/JDA";
   public static final String VERSION_MAJOR = "4";
   public static final String VERSION_MINOR = "2";
   public static final String VERSION_REVISION = "0";
   public static final String VERSION_BUILD = "224";
   public static final String VERSION = "4".startsWith("@") ? "dev" : String.format("%s.%s.%s_%s", "4", "2", "0", "224");
}
