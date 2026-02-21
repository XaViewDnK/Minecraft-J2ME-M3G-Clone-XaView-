import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;
import java.util.Vector;
import javax.microedition.lcdui.*;
import javax.microedition.lcdui.game.*;
import javax.microedition.m3g.*;
import javax.microedition.media.*;
import javax.microedition.media.control.*;
import javax.microedition.midlet.*;
import javax.microedition.rms.*;

public class MinecraftMIDlet extends MIDlet implements javax.microedition.lcdui.CommandListener {
  private static final byte PLATE_STONE = -90, PLATE_GOLD = -91, PLATE_IRON = -92, PLATE_OAK = -93;
  private static final byte LADDER = -117;
  private static final byte CARPET_WHITE = -40,
      CARPET_ORANGE = -41,
      CARPET_MAGENTA = -42,
      CARPET_LIGHT_BLUE = -43,
      CARPET_YELLOW = -44,
      CARPET_LIME = -45,
      CARPET_PINK = -46,
      CARPET_GRAY = -47,
      CARPET_LIGHT_GRAY = -48,
      CARPET_CYAN = -49,
      CARPET_PURPLE = -50,
      CARPET_BLUE = -51,
      CARPET_BROWN = -52,
      CARPET_GREEN = -53,
      CARPET_RED = -54,
      CARPET_BLACK = -55;
  public String playerName = "Steve";
  private javax.microedition.lcdui.TextBox _nickBox;
  private javax.microedition.lcdui.Command _cmdNickOk;
  public MusicSystem musicSys;
  private MCanvas canvas;
  private javax.microedition.lcdui.TextBox _chatBox;
  private javax.microedition.lcdui.TextBox _seedBox;
  private javax.microedition.lcdui.Command _cmdSeedOk;
  private javax.microedition.lcdui.Command _cmdOk, _cmdBack;

  public void commandAction(
      javax.microedition.lcdui.Command c, javax.microedition.lcdui.Displayable d) {
    if (c == _cmdOk) {
      if (canvas != null) canvas.handleChatCommand(_chatBox.getString());
      javax.microedition.lcdui.Display.getDisplay(this).setCurrent(canvas);
    } else if (c == _cmdBack) {
      javax.microedition.lcdui.Display.getDisplay(this).setCurrent(canvas);
    } else if (c == _cmdSeedOk) {
      if (canvas != null) canvas.setSeed(_seedBox.getString());
      javax.microedition.lcdui.Display.getDisplay(this).setCurrent(canvas);
    } else if (c == _cmdNickOk) {
      playerName = _nickBox.getString();
      if (playerName.length() == 0) playerName = "Player";
      saveProfile();
      javax.microedition.lcdui.Display.getDisplay(this).setCurrent(canvas);
    }
  }

  private boolean ignoreBreakCheck = false;

  private void loadProfile() {
    try {
      RecordStore rs = RecordStore.openRecordStore("mc_profile", true);
      if (rs.getNumRecords() > 0) {
        byte[] d = rs.getRecord(1);
        if (d != null) {
          playerName = new String(d);
        }
      }
      rs.closeRecordStore();
    } catch (Exception e) {
    }
  }

  private void saveProfile() {
    try {
      RecordStore rs = RecordStore.openRecordStore("mc_profile", true);
      byte[] d = playerName.getBytes();
      if (rs.getNumRecords() == 0) {
        rs.addRecord(d, 0, d.length);
      } else {
        rs.setRecord(1, d, 0, d.length);
      }
      rs.closeRecordStore();
    } catch (Exception e) {
    }
  }

  public void startApp() {
    loadProfile();
    if (musicSys == null) {
      musicSys = new MusicSystem();
      musicSys.start();
    }
    if (canvas == null) {
      canvas = new MCanvas(this);
      if (_chatBox == null) {
        _chatBox =
            new javax.microedition.lcdui.TextBox(
                "Console", "/", 256, javax.microedition.lcdui.TextField.ANY);
        _cmdOk =
            new javax.microedition.lcdui.Command("Run", javax.microedition.lcdui.Command.OK, 1);
        _cmdBack =
            new javax.microedition.lcdui.Command(
                "Cancel", javax.microedition.lcdui.Command.BACK, 1);
        _chatBox.addCommand(_cmdOk);
        _chatBox.addCommand(_cmdBack);
        _chatBox.setCommandListener(this);
        _seedBox =
            new javax.microedition.lcdui.TextBox(
                "Seed", "", 32, javax.microedition.lcdui.TextField.ANY);
        _cmdSeedOk =
            new javax.microedition.lcdui.Command("OK", javax.microedition.lcdui.Command.OK, 1);
        _seedBox.addCommand(_cmdSeedOk);
        _seedBox.addCommand(_cmdBack);
        _seedBox.setCommandListener(this);
        _nickBox =
            new javax.microedition.lcdui.TextBox(
                "Nickname", playerName, 10, javax.microedition.lcdui.TextField.ANY);
        _cmdNickOk =
            new javax.microedition.lcdui.Command("Save", javax.microedition.lcdui.Command.OK, 1);
        _nickBox.addCommand(_cmdNickOk);
        _nickBox.addCommand(_cmdBack);
        _nickBox.setCommandListener(this);
      }
      Display.getDisplay(this).setCurrent(canvas);
      new Thread(canvas).start();
    }
  }

  public void pauseApp() {
    if (canvas != null) canvas.setPaused(true);
  }

  public void showSeedInput() {
    if (_seedBox != null) javax.microedition.lcdui.Display.getDisplay(this).setCurrent(_seedBox);
  }

  public void destroyApp(boolean u) {
    if (musicSys != null) musicSys.stop();
    if (canvas != null) canvas.stop();
  }

  public void showNickInput() {
    if (_nickBox != null) {
      _nickBox.setString(playerName);
      javax.microedition.lcdui.Display.getDisplay(this).setCurrent(_nickBox);
    }
  }

  public class MusicSystem implements Runnable, PlayerListener {
    public void updateVolume(int v) {
      if (vc != null) vc.setLevel(v);
    }

    private boolean running = true;
    private boolean gameMode = false;
    private boolean curMode = false;
    private Player player;
    private VolumeControl vc;
    private Vector menuTracks = new Vector();
    private Vector gameTracks = new Vector();
    private int lastMenu = -1;
    private int lastGame = -1;
    private long nextPlayTime = 0;
    private Random rand = new Random();

    public MusicSystem() {
      new Thread(this).start();
    }

    public void start() {
      running = true;
    }

    public void stop() {
      running = false;
      closePlayer();
    }

    public void setContext(boolean isGame) {
      if (isGame != gameMode) {
        gameMode = isGame;
      }
    }

    public void run() {
      for (int i = 1; i <= 20; i++)
        if (isValid("/j2me_sounds/menu/" + i + ".mp3")) menuTracks.addElement(new Integer(i));
      for (int i = 1; i <= 20; i++)
        if (isValid("/j2me_sounds/ingame/" + i + ".mp3")) gameTracks.addElement(new Integer(i));
      curMode = false;
      gameMode = false;
      nextPlayTime = System.currentTimeMillis();
      while (running) {
        if (curMode != gameMode) {
          closePlayer();
          curMode = gameMode;
          if (curMode) nextPlayTime = System.currentTimeMillis() + 180000;
          else nextPlayTime = System.currentTimeMillis() + 10000;
        }
        if (System.currentTimeMillis() >= nextPlayTime) {
          if (player == null) {
            playTrack(curMode);
          }
        }
        try {
          Thread.sleep(200);
        } catch (Exception e) {
        }
      }
    }

    private void playTrack(boolean mode) {
      Vector tracks = mode ? gameTracks : menuTracks;
      if (tracks.isEmpty()) return;
      int id = -1;
      int max = tracks.size();
      if (max == 1) {
        id = ((Integer) tracks.elementAt(0)).intValue();
      } else {
        int last = mode ? lastGame : lastMenu;
        for (int k = 0; k < 10; k++) {
          int idx = Math.abs(rand.nextInt()) % max;
          id = ((Integer) tracks.elementAt(idx)).intValue();
          if (id != last) break;
        }
      }
      if (mode) lastGame = id;
      else lastMenu = id;
      String path = "/j2me_sounds/" + (mode ? "ingame/" : "menu/") + id + ".mp3";
      try {
        player = Manager.createPlayer(getClass().getResourceAsStream(path), "audio/mpeg");
        player.addPlayerListener(this);
        player.realize();
        vc = (VolumeControl) player.getControl("VolumeControl");
        if (vc != null) vc.setLevel(0);
        player.start();
        fadeIn();
      } catch (Exception e) {
        if (player != null)
          try {
            player.close();
          } catch (Exception ex) {
          }
        player = null;
        nextPlayTime = System.currentTimeMillis() + 5000;
      }
    }

    private void fadeIn() {
      new Thread(
              new Runnable() {
                public void run() {
                  if (vc == null) return;
                  int target = 100;
                  if (MinecraftMIDlet.this.canvas != null)
                    target = MinecraftMIDlet.this.canvas.setMusic;
                  for (int i = 0; i <= target; i += 5) {
                    if (player == null) return;
                    vc.setLevel(i);
                    try {
                      Thread.sleep(100);
                    } catch (Exception e) {
                    }
                  }
                  if (player != null && vc != null) vc.setLevel(target);
                }
              })
          .start();
    }

    private void closePlayer() {
      if (player != null) {
        if (vc != null) {
          for (int i = 100; i >= 0; i -= 10) {
            vc.setLevel(i);
            try {
              Thread.sleep(50);
            } catch (Exception e) {
            }
          }
        }
        try {
          player.close();
        } catch (Exception e) {
        }
        player = null;
        vc = null;
      }
    }

    public void playerUpdate(Player p, String event, Object eventData) {
      if (event.equals(PlayerListener.END_OF_MEDIA)) {
        closePlayer();
        if (curMode) nextPlayTime = System.currentTimeMillis() + 180000;
        else nextPlayTime = System.currentTimeMillis() + 10000;
      }
    }

    private boolean isValid(String path) {
      try {
        java.io.InputStream is = getClass().getResourceAsStream(path);
        if (is != null) {
          is.close();
          return true;
        }
      } catch (Exception e) {
      }
      return false;
    }
  }

  class MCanvas extends GameCanvas implements Runnable {
    public boolean showStructureLocator = false;
    public int locVilX = 0, locVilZ = 0;
    public boolean locHasVil = false;
    public int locFortX = 0, locFortZ = 0;
    public boolean locHasFort = false;

    private double Math_atan2(double y, double x) {
      if (x == 0.0 && y == 0.0) return 0.0;
      double ax = Math.abs(x);
      double ay = Math.abs(y);
      double a = (ax < ay) ? ax / ay : ay / ax;
      double s = a * a;
      double r = ((-0.0464964749 * s + 0.15931422) * s - 0.327622764) * s * a + a;
      if (ay > ax) r = 1.57079637 - r;
      if (x < 0) r = 3.14159274 - r;
      return (y < 0) ? -r : r;
    }

    private Image imgBubblePop, imgBubble;
    private Image imgArmorHalf, imgArmorFull;
    private Image imgHpEmpty, imgHpHalf, imgHpFull;
    private Image imgHungerEmpty, imgHungerHalf, imgHungerFull;
    private Image imgArmorHelmet, imgArmorChest, imgArmorLegs, imgArmorBoots;
    private static final byte GRASS_PATH = -118;
    private String loadingTip = "Loading...";
    private String[] loadingTips = {
      "To open inventory press # button",
      "To open pause press # button",
      "To open chat press R button",
      "To low flight press L button"
    };

    private void drawLoading(float progress) {
      Graphics g = getGraphics();
      int w = getWidth();
      int h = getHeight();
      g.setColor(0x000000);
      g.fillRect(0, 0, w, h);
      g.setColor(0xFFFFFF);
      g.setFont(btnFont);
      String title = "Loading...";
      g.drawString(title, w / 2, h / 2 - 40, Graphics.HCENTER | Graphics.TOP);
      int barW = w - 40;
      int barH = 10;
      int bx = 20;
      int by = h / 2;
      g.setColor(0xFFFFFF);
      g.drawRect(bx, by, barW, barH);
      g.setColor(0x00FF00);
      int fillW = (int) (barW * progress);
      if (fillW > barW - 2) fillW = barW - 2;
      if (fillW > 0) g.fillRect(bx + 1, by + 1, fillW, barH - 1);
      g.setFont(debugFont);
      g.setColor(0xAAAAAA);
      if (loadingTip != null) {
        int tipW = debugFont.stringWidth(loadingTip);
        g.drawString(loadingTip, w / 2, by + 20, Graphics.HCENTER | Graphics.TOP);
      }
      flushGraphics();
    }

    private Material cloudMat;
    public long worldTime = 0;
    private int currentSkyColor = 0x87CEEB;
    private float dayLightFactor = 1.0f;

    private void updateTimeOfDay(int dt) {
      if (currentDim == -1) {
        currentSkyColor = 0x2A0000;
        dayLightFactor = 0.5f;
        return;
      }
      worldTime = (worldTime + dt / 50) % 24000;
      int r1 = 0x87, g1 = 0xCE, b1 = 0xEB;
      int r2 = 0x05, g2 = 0x05, b2 = 0x15;
      if (worldTime < 12000) {
        currentSkyColor = 0x87CEEB;
        dayLightFactor = 1.0f;
      } else if (worldTime < 13000) {
        float f = (worldTime - 12000) / 1000.0f;
        int r = (int) (r1 + (r2 - r1) * f);
        int g = (int) (g1 + (g2 - g1) * f);
        int b = (int) (b1 + (b2 - b1) * f);
        currentSkyColor = (r << 16) | (g << 8) | b;
        dayLightFactor = 1.0f - (0.8f * f);
      } else if (worldTime < 23000) {
        currentSkyColor = 0x050515;
        dayLightFactor = 0.2f;
      } else {
        float f = (worldTime - 23000) / 1000.0f;
        int r = (int) (r2 + (r1 - r2) * f);
        int g = (int) (g2 + (g1 - g2) * f);
        int b = (int) (b2 + (b1 - b2) * f);
        currentSkyColor = (r << 16) | (g << 8) | b;
        dayLightFactor = 0.2f + (0.8f * f);
      }
      if (cloudMat != null) {
        int v = (int) (255 * dayLightFactor);
        if (v > 255) v = 255;
        if (v < 0) v = 0;
        cloudMat.setColor(Material.EMISSIVE, 0xFF000000 | (v << 16) | (v << 8) | v);
      }
    }

    private Image scaledLogo = null;
    private int lastScW = -1;

    public boolean isDirectional(byte id) {
      return id == LADDER
          || id == FURNACE
          || id == CHEST
          || id == WORKBENCH
          || id == PUMPKIN
          || id == JACK_O_LANTERN
          || id == IRON_BARS
          || id == GLASS_PANE;
    }

    public int getRotatedFace(int face, int data, byte id) {
      if (!isDirectional(id)) return face;
      if (face < 2) return face;
      int dir = data & 3;
      int targetFace = 2;
      if (dir == 0) targetFace = 2;
      else if (dir == 1) targetFace = 4;
      else if (dir == 2) targetFace = 3;
      else if (dir == 3) targetFace = 5;
      if (face == targetFace) return 2;
      if (face == 2) return targetFace;
      return face;
    }

    private Image profileImg;

    class ChatMsg {
      String text;
      int timer;

      public ChatMsg(String t, int time) {
        text = t;
        timer = time;
      }
    }

    private Vector chatLog = new Vector();

    private void addChatMessage(String msg) {
      if (msg == null || msg.length() == 0) return;
      int duration = 300;
      if (msg.indexOf("Invalid command") != -1) {
        duration = 150;
      }
      int maxW = getWidth() - 6;
      if (maxW < 10) maxW = 100;
      Font f = debugFont;
      int len = msg.length();
      int start = 0;
      while (start < len) {
        int end = len;
        while (f.substringWidth(msg, start, end - start) > maxW && end > start) {
          end--;
        }
        if (end < len) {
          int space = msg.lastIndexOf(' ', end);
          if (space > start) {
            end = space;
          }
        }
        if (end == start) end = start + 1;
        String sub = msg.substring(start, end).trim();
        if (sub.length() > 0) {
          chatLog.addElement(new ChatMsg(sub, duration));
        }
        start = end;
      }
    }

    private boolean isSprinting = false;
    private long lastForwardTime = 0;
    private boolean spectatorMode = false;
    private Appearance appItemTop, appItemBot, appItemFront, appItemBack, appItemLeft, appItemRight;
    private int settingsPage = 0;
    private boolean showFPS = true;
    private boolean showXYZ = true;
    private long lastNavTime = 0;
    private long lastActionTime = 0;
    private Mesh panoMesh;
    private Camera panoCam;
    private float panoRot = 0.0f;
    private boolean panoInit = false;
    private Image logoImg;

    private void loadSettings() {
      try {
        RecordStore rs = RecordStore.openRecordStore("mc_cnf", true);
        if (rs.getNumRecords() > 0) {
          byte[] d = rs.getRecord(1);
          if (d != null && d.length >= 7) {
            setDrops = d[0];
            setLiquid = d[1];
            setClouds = d[2];
            setEffects = d[3];
            setupMode = d[4];
            setMusic = d[5];
            if (setMusic == 1) setMusic = 100;
            setAnimations = d[6];
          }
          if (d != null && d.length >= 9) {
            showFPS = (d[7] != 0);
            showXYZ = (d[8] != 0);
          }
          if (d != null && d.length >= 10) {
            setChunks = d[9];
            if (setChunks < 1) setChunks = 1;
          }
        }
        rs.closeRecordStore();
      } catch (Exception e) {
      }
    }

    private void saveSettings() {
      try {
        RecordStore rs = RecordStore.openRecordStore("mc_cnf", true);
        byte[] d = {
          (byte) setDrops,
          (byte) setLiquid,
          (byte) setClouds,
          (byte) setEffects,
          (byte) setupMode,
          (byte) setMusic,
          (byte) setAnimations,
          (byte) (showFPS ? 1 : 0),
          (byte) (showXYZ ? 1 : 0),
          (byte) setChunks
        };
        if (rs.getNumRecords() == 0) rs.addRecord(d, 0, d.length);
        else rs.setRecord(1, d, 0, d.length);
        rs.closeRecordStore();
      } catch (Exception e) {
      }
      if (MinecraftMIDlet.this.musicSys != null)
        MinecraftMIDlet.this.musicSys.updateVolume(setMusic);
    }

    private int currentDim = 0;
    private Hashtable texCache = new Hashtable();
    private Hashtable imgCache = new Hashtable();
    private Vector missingCache = new Vector();

    public String getTexName(byte id, int face) {
      if (id == GRASS_PATH) {
        if (face == 0) return "grass_path_top";
        if (face == 1) return "grass_path_bottom";
        return "grass_path_side";
      }
      if (id == BED_BLOCK) return "wool_colored_red";
      if (id >= CARPET_BLACK && id <= CARPET_WHITE) {
        byte woolId = (byte) (id + 30);
        return getTexName(woolId, face);
      }
      if (id == WOOL_WHITE) return "wool_colored_white";
      if (id == WOOL_ORANGE) return "wool_colored_orange";
      if (id == WOOL_MAGENTA) return "wool_colored_magenta";
      if (id == WOOL_LIGHT_BLUE) return "wool_colored_light_blue";
      if (id == WOOL_YELLOW) return "wool_colored_yellow";
      if (id == WOOL_LIME) return "wool_colored_lime";
      if (id == WOOL_PINK) return "wool_colored_pink";
      if (id == WOOL_GRAY) return "wool_colored_gray";
      if (id == WOOL_LIGHT_GRAY) return "wool_colored_silver";
      if (id == WOOL_CYAN) return "wool_colored_cyan";
      if (id == WOOL_PURPLE) return "wool_colored_purple";
      if (id == WOOL_BLUE) return "wool_colored_blue";
      if (id == WOOL_BROWN) return "wool_colored_brown";
      if (id == WOOL_GREEN) return "wool_colored_green";
      if (id == WOOL_RED) return "wool_colored_red";
      if (id == WOOL_BLACK) return "wool_colored_black";
      if (id == PLATE_STONE) return "plate_stone";
      if (id == PLATE_OAK) return "plate_oak";
      if (id == LADDER) return "ladder";
      if (id == PLATE_GOLD) return "plate_gold";
      if (id == PLATE_IRON) return "plate_iron";
      if (id == IRON_BARS) return "iron_bars";
      if (id == GLASS_PANE) return "glass";
      if (id == TORCH) return "torch";
      if (id == STAIRS_WOOD) return "oak_stairs";
      if (id == STAIRS_COBBLE) return "cobble_stairs";
      if (id == FENCE) return "oak_fence";
      if (id == SLAB_COBBLE) return "cobblestone";
      if (id == SLAB_OAK) return "planks";
      if (id == BOOKSHELF) {
        if (face == 0 || face == 1) return "planks";
        return "bookshelf";
      }
      if (id == 65) return "fire";
      String name = getItemName(id).replace(' ', '_').toLowerCase();
      if (id == 1) {
        if (face == 0) return name + "_top";
        if (face == 1) return name + "_bottom";
        return name + "_side";
      }
      if (id == 10) {
        if (face == 0) return name + "_top";
        if (face == 1) return name + "_bottom";
        if (face == 2 || face == 3) return name + "_frontandback";
        return name + "_side";
      }
      if (id == 16 || id == 81) {
        if (face == 0 || face == 1) return name + "_topandbottom";
        if (face == 2) return name + "_front";
        return name + "_side";
      }
      if (id == 104) {
        if (face == 0 || face == 1) return name + "_topandbottom";
        return name + "_side";
      }
      if (id == 105) {
        if (face == 0 || face == 1) return name + "_topandbottom";
        if (face == 2) return name + "_front";
        return name + "_side";
      }
      if (id == 4 || id == 109 || id == 112 || id == 115 || id == 118 || id == 121) {
        if (face == 0 || face == 1) return name + "_topandbottom";
        return name + "_side";
      }
      if (id == 63 || id == 94) {
        if (face == 0) return name + "_top";
        if (face == 1) return name + "_bottom";
        return name + "_side";
      }
      return name;
    }

    public Image getTexImage(byte id) {
      if (id == 0) return null;
      String name = getItemName(id).replace(' ', '_').toLowerCase();
      if (imgCache.containsKey(name)) return (Image) imgCache.get(name);
      getTex(name);
      if (imgCache.containsKey(name)) return (Image) imgCache.get(name);
      return null;
    }

    public Texture2D getTexByBlockId(byte id) {
      if (id == 0) return null;
      int face = 4;
      if (id == 16 || id == 81 || id == 10 || id == 105) face = 2;
      String name = getTexName(id, face);
      return getTex(name);
    }

    public Texture2D getTex(String name) {
      if (name.equals("portal_anim")) {
        if (pTex == null) initPortalAnim();
        return pTex;
      }
      if (texCache.containsKey(name)) return (Texture2D) texCache.get(name);
      if (name.equals("flash")) return null;
      if (missingCache.contains(name)) return null;
      try {
        InputStream is = getClass().getResourceAsStream("/j2me_textures/" + name + ".png");
        if (is != null) {
          Image i = Image.createImage(is);
          Texture2D t = new Texture2D(new Image2D(Image2D.RGBA, i));
          t.setBlending(Texture2D.FUNC_MODULATE);
          t.setFiltering(Texture2D.FILTER_NEAREST, Texture2D.FILTER_NEAREST);
          t.setWrapping(Texture2D.WRAP_CLAMP, Texture2D.WRAP_CLAMP);
          texCache.put(name, t);
          imgCache.put(name, i);
          return t;
        }
      } catch (Exception e) {
      }
      missingCache.addElement(name);
      return null;
    }

    private float portalTimer = 0;
    private boolean wasInPortal = false;
    private byte[] backupWorld, backupData;
    private float[] backupPos = new float[5];
    private boolean netherGenerated = false;

    private byte getBlockIdFromName(String s) {
      if (s.equals("torch")) return TORCH;
      try {
        return Byte.parseByte(s);
      } catch (Exception e) {
      }
      s = s.trim().toLowerCase();
      if (s.equals("air")) return 0;
      if (s.equals("stone")) return 7;
      if (s.equals("grass")) return 1;
      if (s.equals("grass_block")) return 1;
      if (s.equals("dirt")) return 2;
      if (s.equals("cobblestone")) return 3;
      if (s.equals("planks")) return 9;
      if (s.equals("oak_planks")) return 9;
      if (s.equals("log")) return 4;
      if (s.equals("oak_log")) return 4;
      if (s.equals("leaves")) return 5;
      if (s.equals("oak_leaves")) return 5;
      if (s.equals("bedrock")) return 6;
      if (s.equals("water")) return 22;
      if (s.equals("lava")) return 27;
      if (s.equals("sand")) return 17;
      if (s.equals("gravel")) return 18;
      if (s.equals("gold_ore")) return 21;
      if (s.equals("iron_ore")) return 20;
      if (s.equals("coal_ore")) return 19;
      if (s.equals("wood")) return 4;
      if (s.equals("glass")) return 24;
      if (s.equals("diamond_ore")) return 25;
      if (s.equals("crafting_table")) return 10;
      if (s.equals("furnace")) return 16;
      if (s.equals("tnt")) return 63;
      if (s.equals("obsidian")) return 30;
      if (s.equals("fire")) return 65;
      if (s.equals("spruce_log")) return 112;
      if (s.equals("birch_log")) return 109;
      if (s.equals("jungle_log")) return 115;
      if (s.equals("sandstone")) return 94;
      if (s.equals("redstone_ore")) return 82;
      if (s.equals("snow_block")) return 97;
      if (s.equals("ice")) return 96;
      if (s.equals("cactus")) return 103;
      if (s.equals("clay")) return 95;
      if (s.equals("pumpkin")) return 104;
      if (s.equals("jack_o_lantern")) return 105;
      if (s.equals("netherrack")) return 72;
      if (s.equals("soul_sand")) return 73;
      if (s.equals("glowstone")) return 75;
      if (s.equals("quartz_ore")) return 77;
      if (s.equals("nether_brick")) return 89;
      if (s.equals("cobweb")) return 106;
      if (s.equals("stick")) return 11;
      if (s.equals("wood_pickaxe")) return 12;
      if (s.equals("wood_axe")) return 13;
      if (s.equals("wood_shovel")) return 14;
      if (s.equals("wood_sword")) return 15;
      if (s.equals("diamond")) return 26;
      if (s.equals("flint")) return 29;
      if (s.equals("coal")) return 31;
      if (s.equals("charcoal")) return 32;
      if (s.equals("iron_ingot")) return 33;
      if (s.equals("gold_ingot")) return 34;
      if (s.equals("iron_pickaxe")) return 35;
      if (s.equals("iron_axe")) return 36;
      if (s.equals("iron_shovel")) return 37;
      if (s.equals("iron_sword")) return 38;
      if (s.equals("gold_pickaxe")) return 39;
      if (s.equals("gold_axe")) return 40;
      if (s.equals("gold_shovel")) return 41;
      if (s.equals("gold_sword")) return 42;
      if (s.equals("diamond_pickaxe")) return 43;
      if (s.equals("diamond_axe")) return 44;
      if (s.equals("diamond_shovel")) return 45;
      if (s.equals("diamond_sword")) return 46;
      if (s.equals("emerald")) return 47;
      if (s.equals("lapis")) return 48;
      if (s.equals("emerald_ore")) return 49;
      if (s.equals("lapis_ore")) return 50;
      if (s.equals("iron_helmet")) return 51;
      if (s.equals("iron_chestplate")) return 52;
      if (s.equals("iron_leggings")) return 53;
      if (s.equals("iron_boots")) return 54;
      if (s.equals("gold_helmet")) return 55;
      if (s.equals("gold_chestplate")) return 56;
      if (s.equals("gold_leggings")) return 57;
      if (s.equals("gold_boots")) return 58;
      if (s.equals("diamond_helmet")) return 59;
      if (s.equals("diamond_chestplate")) return 60;
      if (s.equals("diamond_leggings")) return 61;
      if (s.equals("diamond_boots")) return 62;
      if (s.equals("flint_and_steel")) return 64;
      if (s.equals("farmland")) return 67;
      if (s.equals("wood_hoe")) return 68;
      if (s.equals("iron_hoe")) return 69;
      if (s.equals("gold_hoe")) return 70;
      if (s.equals("diamond_hoe")) return 71;
      if (s.equals("magma")) return 74;
      if (s.equals("glowstone_dust")) return 76;
      if (s.equals("quartz")) return 78;
      if (s.equals("red_mushroom")) return 79;
      if (s.equals("brown_mushroom")) return 80;
      if (s.equals("chest")) return 81;
      if (s.equals("redstone")) return 83;
      if (s.equals("bucket")) return 84;
      if (s.equals("water_bucket")) return 85;
      if (s.equals("lava_bucket")) return 86;
      if (s.equals("stone_pickaxe")) return 87;
      if (s.equals("door")) return 88;
      if (s.equals("wood_door")) return 88;
      if (s.equals("nether_fence")) return 90;
      if (s.equals("nether_stairs")) return 91;
      if (s.equals("nether_wart")) return 92;
      if (s.equals("barrier")) return 93;
      if (s.equals("grass_plant")) return 98;
      if (s.equals("tall_grass")) return 99;
      if (s.equals("dandelion")) return 100;
      if (s.equals("rose")) return 101;
      if (s.equals("reeds")) return 102;
      if (s.equals("sugar_cane")) return 102;
      if (s.equals("dead_bush")) return 107;
      if (s.equals("snow_layer")) return 108;
      if (s.equals("birch_planks")) return 110;
      if (s.equals("birch_leaves")) return 111;
      if (s.equals("spruce_planks")) return 113;
      if (s.equals("spruce_leaves")) return 114;
      if (s.equals("jungle_planks")) return 116;
      if (s.equals("jungle_leaves")) return 117;
      if (s.equals("acacia_log")) return 118;
      if (s.equals("acacia_planks")) return 119;
      if (s.equals("acacia_leaves")) return 120;
      if (s.equals("dark_oak_log")) return 121;
      if (s.equals("dark_oak_planks")) return 122;
      if (s.equals("dark_oak_leaves")) return 123;
      return 0;
    }

    public void handleChatCommand(String cmd) {
      if (cmd == null || cmd.length() == 0) return;
      if (!cmd.startsWith("/")) {
        addChatMessage("<" + MinecraftMIDlet.this.playerName + "> " + cmd);
        javax.microedition.lcdui.Display.getDisplay(MinecraftMIDlet.this).setCurrent(this);
        return;
      }
      boolean isValid = false;
      String lower = cmd.toLowerCase();
      try {
        if (lower.startsWith("/give @s ")) {
          String args = cmd.substring(9).trim();
          int sp = args.indexOf(' ');
          String sId = (sp == -1) ? args : args.substring(0, sp);
          String sCnt = (sp == -1) ? "1" : args.substring(sp + 1).trim();
          byte id = getBlockIdFromName(sId);
          int count = Integer.parseInt(sCnt);
          if (id != 0 && count > 0) {
            for (int i = 0; i < 36 && count > 0; i++) {
              Slot s = (i < 9) ? hotbar[i] : inventory[i - 9];
              if (s.id == id && s.count < 64) {
                int add = Math.min(count, 64 - s.count);
                s.count += add;
                count -= add;
              }
            }
            for (int i = 0; i < 36 && count > 0; i++) {
              Slot s = (i < 9) ? hotbar[i] : inventory[i - 9];
              if (s.count == 0) {
                s.id = id;
                int add = Math.min(count, 64);
                s.count = add;
                count -= add;
              }
            }
            while (count > 0) {
              int drop = Math.min(count, 64);
              drops.addElement(new Drop(px, py, pz, id, 0f, 0f, 0f, drop, 1000));
              count -= drop;
            }
            updateCrafting();
            isValid = true;
          }
        } else if (lower.startsWith("/setblock ")) {
          int sp1 = cmd.indexOf(' ', 10);
          int sp2 = (sp1 > 0) ? cmd.indexOf(' ', sp1 + 1) : -1;
          int sp3 = (sp2 > 0) ? cmd.indexOf(' ', sp2 + 1) : -1;
          if (sp1 > 0 && sp2 > 0 && sp3 > 0) {
            String sX = cmd.substring(10, sp1).trim();
            String sY = cmd.substring(sp1 + 1, sp2).trim();
            String sZ = cmd.substring(sp2 + 1, sp3).trim();
            String sId = cmd.substring(sp3 + 1).trim();
            int x = Integer.parseInt(sX);
            int y = Integer.parseInt(sY);
            int z = Integer.parseInt(sZ);
            byte b = getBlockIdFromName(sId);
            setBlockAndDirty(x, y, z, b);
            isValid = true;
          }
        } else if (lower.startsWith("/gamemode 1") || lower.indexOf("creative") != -1) {
          creativeMode = true;
          spectatorMode = false;
          isFlying = true;
          isValid = true;
        } else if (lower.startsWith("/gamemode 3") || lower.indexOf("spectator") != -1) {
          creativeMode = true;
          spectatorMode = true;
          isFlying = true;
          isValid = true;
        } else if (lower.startsWith("/gamemode 0") || lower.indexOf("survival") != -1) {
          creativeMode = false;
          spectatorMode = false;
          isFlying = false;
          if (py < 0) py = 50;
          isValid = true;
        } else if (lower.startsWith("/time set ")) {
          String t = lower.substring(10).trim();
          if (t.equals("day")) worldTime = 0;
          else if (t.equals("noon")) worldTime = 6000;
          else if (t.equals("sunset")) worldTime = 12000;
          else if (t.equals("night")) worldTime = 13000;
          else if (t.equals("midnight")) worldTime = 18000;
          else if (t.equals("sunrise")) worldTime = 23000;
          else {
            try {
              worldTime = Long.parseLong(t) % 24000;
            } catch (Exception ex) {
              addChatMessage("Invalid time format");
              isValid = false;
            }
          }
          if (isValid) {
            addChatMessage("Time set to " + worldTime);
            updateTimeOfDay(0);
          }
          isValid = true;
        } else if (lower.equals("/f3")) {
          showStructureLocator = !showStructureLocator;
          addChatMessage("Locator: " + (showStructureLocator ? "ON" : "OFF"));
          isValid = true;
        } else if (lower.startsWith("/locate structure ")) {
          String type = lower.substring(18).trim();
          if (type.equals("village")) {
            if (currentDim == 0) {
              if (locHasVil) addChatMessage("Village at " + locVilX + ", " + locVilZ);
              else addChatMessage("No village found in cache.");
            } else addChatMessage("Villages are in the Overworld.");
            isValid = true;
          } else if (type.equals("fortress")) {
            if (currentDim == -1) {
              if (locHasFort) addChatMessage("Fortress at " + locFortX + ", " + locFortZ);
              else addChatMessage("No fortress found in cache.");
            } else addChatMessage("Fortresses are in the Nether.");
            isValid = true;
          }
        }
      } catch (Exception e) {
        isValid = false;
      }
      if (!isValid) {
        addChatMessage("Invalid command");
      }
      javax.microedition.lcdui.Display.getDisplay(MinecraftMIDlet.this).setCurrent(this);
    }

    private boolean running = true;
    private static final int CHUNK_SIZE = 16,
        CHUNKS_X = 16,
        CHUNKS_Z = 16,
        WORLD_X = CHUNK_SIZE * CHUNKS_X,
        WORLD_Y = CHUNK_SIZE * CHUNKS_Z,
        WORLD_H = 64,
        SEA_LEVEL = 28,
        V_SCALE = 16,
        CLOUD_H = 48,
        CLOUD_RES = 32,
        CLOUD_SCALE = WORLD_X / CLOUD_RES;
    private static final int SEC_HOTBAR = 0,
        SEC_INV = 1,
        SEC_ARMOR = 2,
        SEC_CRAFT = 3,
        SEC_RESULT = 4,
        SEC_FURNACE_IN = 10,
        SEC_FURNACE_FUEL = 11,
        SEC_FURNACE_OUT = 12,
        SEC_TABS = 99,
        SEC_LIB = 100,
        SEC_DELETE = 101;
    private static final byte AIR = 0,
        GRASS = 1,
        DIRT = 2,
        COBBLE = 3,
        WOOD = 4,
        LEAVES = 5,
        BEDROCK = 6,
        STONE = 7,
        CLOUD = 8,
        PLANKS = 9,
        WORKBENCH = 10,
        STICK = 11,
        WOOD_PICKAXE = 12,
        WOOD_AXE = 13,
        WOOD_SHOVEL = 14,
        WOOD_SWORD = 15,
        FURNACE = 16,
        SAND = 17,
        GRAVEL = 18,
        ORE_COAL = 19,
        ORE_IRON = 20,
        ORE_GOLD = 21,
        WATER = 22,
        WATER_FLOW = 23,
        GLASS = 24,
        ORE_DIAMOND = 25,
        DIAMOND = 26,
        LAVA = 27,
        LAVA_FLOW = 28,
        FLINT = 29,
        OBSIDIAN = 30,
        COAL = 31,
        CHARCOAL = 32,
        IRON_INGOT = 33,
        GOLD_INGOT = 34,
        IRON_PICKAXE = 35,
        IRON_AXE = 36,
        IRON_SHOVEL = 37,
        IRON_SWORD = 38,
        GOLD_PICKAXE = 39,
        GOLD_AXE = 40,
        GOLD_SHOVEL = 41,
        GOLD_SWORD = 42,
        DIAMOND_PICKAXE = 43,
        DIAMOND_AXE = 44,
        DIAMOND_SHOVEL = 45,
        DIAMOND_SWORD = 46,
        EMERALD = 47,
        LAPIS = 48,
        ORE_EMERALD = 49,
        ORE_LAPIS = 50,
        HELMET_IRON = 51,
        CHESTPLATE_IRON = 52,
        LEGGINGS_IRON = 53,
        BOOTS_IRON = 54,
        HELMET_GOLD = 55,
        CHESTPLATE_GOLD = 56,
        LEGGINGS_GOLD = 57,
        BOOTS_GOLD = 58,
        HELMET_DIAMOND = 59,
        CHESTPLATE_DIAMOND = 60,
        LEGGINGS_DIAMOND = 61,
        BOOTS_DIAMOND = 62,
        TNT = 63,
        FLINT_AND_STEEL = 64,
        FIRE = 65,
        PORTAL = 66,
        FARMLAND = 67,
        WOOD_HOE = 68,
        IRON_HOE = 69,
        GOLD_HOE = 70,
        DIAMOND_HOE = 71,
        NETHERRACK = 72,
        SOUL_SAND = 73,
        MAGMA = 74,
        GLOWSTONE = 75,
        GLOWSTONE_DUST = 76,
        ORE_QUARTZ = 77,
        QUARTZ = 78,
        MUSHROOM_RED = 79,
        MUSHROOM_BROWN = 80,
        CHEST = 81,
        ORE_REDSTONE = 82,
        REDSTONE = 83,
        BUCKET = 84,
        BUCKET_WATER = 85,
        BUCKET_LAVA = 86,
        STONE_PICKAXE = 87,
        WOOD_DOOR = 88,
        NETHER_BRICK = 89,
        NETHER_FENCE = 90,
        NETHER_STAIRS = 91,
        NETHER_WART = 92,
        BARRIER = 93,
        SANDSTONE = 94,
        CLAY = 95,
        ICE = 96,
        SNOW_BLOCK = 97,
        SHORT_GRASS = 98,
        PLANT_TALL_GRASS = 99,
        FLOWER_YELLOW = 100,
        FLOWER_RED = 101,
        REEDS = 102,
        CACTUS = 103,
        PUMPKIN = 104,
        JACK_O_LANTERN = 105,
        WEB = 106,
        DEAD_BUSH = 107,
        SNOW_LAYER = 108,
        WOOD_BIRCH = 109,
        PLANKS_BIRCH = 110,
        LEAVES_BIRCH = 111,
        WOOD_SPRUCE = 112,
        PLANKS_SPRUCE = 113,
        LEAVES_SPRUCE = 114,
        WOOD_JUNGLE = 115,
        PLANKS_JUNGLE = 116,
        LEAVES_JUNGLE = 117,
        WOOD_ACACIA = 118,
        PLANKS_ACACIA = 119,
        LEAVES_ACACIA = 120,
        WOOD_DARK_OAK = 121,
        PLANKS_DARK_OAK = 122,
        LEAVES_DARK_OAK = 123,
        SHEARS = 124,
        WHEAT_SEEDS = 125,
        WHEAT_BLOCK = 126,
        WHEAT = 127,
        BREAD = -128,
        STONE_AXE = -127,
        STONE_SHOVEL = -126,
        STONE_SWORD = -125,
        STONE_HOE = -124,
        BLOCK_COAL = -100,
        BLOCK_IRON = -101,
        BLOCK_GOLD = -102,
        BLOCK_DIAMOND = -103,
        BLOCK_EMERALD = -104,
        BLOCK_LAPIS = -105,
        BLOCK_REDSTONE = -106,
        BLOCK_QUARTZ = -107,
        STAIRS_WOOD = -108,
        STAIRS_COBBLE = -109,
        FENCE = -110,
        BOOKSHELF = -111,
        TORCH = -112,
        IRON_BARS = -113,
        GLASS_PANE = -114,
        SLAB_COBBLE = -115,
        SLAB_OAK = -116;
    private static final byte BED_BLOCK = -60;
    private static final byte WOOL_WHITE = -10,
        WOOL_ORANGE = -11,
        WOOL_MAGENTA = -12,
        WOOL_LIGHT_BLUE = -13,
        WOOL_YELLOW = -14,
        WOOL_LIME = -15,
        WOOL_PINK = -16,
        WOOL_GRAY = -17,
        WOOL_LIGHT_GRAY = -18,
        WOOL_CYAN = -19,
        WOOL_PURPLE = -20,
        WOOL_BLUE = -21,
        WOOL_BROWN = -22,
        WOOL_GREEN = -23,
        WOOL_RED = -24,
        WOOL_BLACK = -25;
    private int gameState = 0;
    private byte[] world, worldData;
    private byte[] mapLight;
    private int[] lightRamp;
    private boolean lightInit = false;

    private void initLightRamp() {
      if (lightRamp != null) return;
      lightRamp = new int[16];
      for (int i = 0; i <= 15; i++) {
        float f = (float) i / 15.0f;
        float val = (float) (f * Math.sqrt(f));
        if (i > 0 && val < 0.03f) val = 0.03f;
        if (i == 0) val = 0.0f;
        int c = (int) (val * 255);
        if (c > 255) c = 255;
        else if (c < 0) c = 0;
        lightRamp[i] = c;
      }
      setLight = 2;
    }

    private Chunk[] chunks;
    public String currentSeed = "";

    public void setSeed(String s) {
      currentSeed = s;
    }

    private long getSeedLong() {
      if (currentSeed == null || currentSeed.length() == 0) return System.currentTimeMillis();
      int h = 0;
      for (int i = 0; i < currentSeed.length(); i++) h = 31 * h + currentSeed.charAt(i);
      return (long) h;
    }

    private int setupType = 0;
    private int menuSelection = 0,
        setDrops = 0,
        setLiquid = 0,
        setClouds = 0,
        setupMode = 0,
        setEffects = 0;
    public int setMusic = 100;
    private int setAnimations = 1, setLight = 0, setChunks = 4;
    private Image[] pFrms;
    private Image2D[] pImg2Ds;
    private Texture2D pTex;
    private int pTick = 0;
    private Mesh crossedMesh;

    private boolean isCrossed(byte id) {
      if (id == TORCH) return true;
      return id == FIRE
          || id == MUSHROOM_RED
          || id == MUSHROOM_BROWN
          || id == REEDS
          || id == PLANT_TALL_GRASS
          || id == SHORT_GRASS
          || id == NETHER_WART
          || id == FLOWER_RED
          || id == FLOWER_YELLOW
          || id == DEAD_BUSH
          || id == WEB;
    }

    private boolean creativeMode = false, isFlying = false;
    private long lastJumpTime = 0;
    private boolean k_up,
        k_down,
        k_left,
        k_right,
        k_2,
        k_4,
        k_6,
        k_8,
        k_0,
        k_1,
        k_3,
        k_7,
        k_9,
        k_lsk,
        k_star,
        k_fire,
        k_pound,
        k_5;
    private boolean isMining = false;
    private float miningProgress = 0.0f;
    private int miningX, miningY, miningZ;
    private int health = 20, food = 20, air = 300;
    private float foodExhaustion = 0;
    private int healTimer = 0, damageTimer = 0;
    private long dropHoldStart = 0;
    private boolean droppedInitial = false;
    private Graphics3D g3d;
    private Camera camera;
    private Background background;
    private Light globalLight;
    private Transform tCam = new Transform(), tGlobal = new Transform(), tHand = new Transform();
    private Appearance appWorld, appClouds, appDrop, appShadow, appSel, appHand;
    private Appearance[] appCracks;
    private Texture2D texBorder, texShadow;
    private Material matMain;
    private Mesh cloudMesh3D, cloudMesh2D, selMesh, handMesh, crackMesh, itemMesh;
    private float cloudOffset = 0, animTime = 0, handSwing = 0.0f;
    private boolean isSwinging = false;
    private float walkBob = 0.0f;
    private static final int MAX_VERTS = 45000;
    private short[] tmpVerts = new short[MAX_VERTS * 3], tmpTexs = new short[MAX_VERTS * 2];
    private byte[] tmpCols = new byte[MAX_VERTS * 3];
    private int[] tmpIndices = new int[MAX_VERTS * 3 / 2];
    private float px = 128.5f, py = 40.0f, pz = 128.5f, ry = 0.0f, rx = 0.0f, vy = 0.0f;
    private boolean onGround = false;
    private float fallStartY = 0;
    private final float PLAYER_R = 0.3f, PLAYER_H = 1.8f;
    private float[] flowVec = new float[2];

    class Slot {
      byte id = 0;
      int count = 0;
    }

    private Slot[] hotbar = new Slot[9],
        inventory = new Slot[27],
        armor = new Slot[4],
        craft = new Slot[9];
    private Hashtable tileEntities = new Hashtable();
    private Slot[] chestInv;
    private int chestRows = 0;
    private static final int SEC_CHEST = 105;
    private Slot craftResult = new Slot(),
        furnaceIn = new Slot(),
        furnaceFuel = new Slot(),
        furnaceOut = new Slot(),
        cursor = new Slot();
    private int burnTime = 0,
        burnTimeMax = 0,
        cookTime = 0,
        selectedSlot = 0,
        invCursorX = 0,
        invCursorY = 0,
        invSection = 0;
    private long lastClickTime = 0;
    private boolean isDragging = false;
    private Vector dragSlots = new Vector();
    private String tooltipName = "";
    private int tooltipTimer = 0;
    private int slSz = 20, guiOx = 0, guiOy = 0;
    private Vector drops = new Vector();
    private Vector fallingBlocks = new Vector();

    private class FallingBlock {
      float x, y, z, vy;
      byte type;

      public FallingBlock(float x, float y, float z, byte t) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.type = t;
      }
    }

    private int fps = 0, frameCount = 0;
    private long lastTime = 0;
    private Font debugFont, btnFont, invFont;
    private int targetX, targetY, targetZ, lastX, lastY, lastZ;
    private boolean hasTarget = false;
    private VertexBuffer dropVB, dropFlatVB, shadowVB;
    private IndexBuffer dropIB, dropFlatIB, shadowIB;
    private int eatTimer = 0;
    private int fluidTickTimer = 0;
    private int fireTickTimer = 0;
    private Random rand = new Random();
    private int creativeTab = 0;
    private byte[] libItems = {
      1,
      2,
      3,
      4,
      5,
      6,
      7,
      9,
      10,
      11,
      12,
      13,
      14,
      15,
      16,
      17,
      18,
      19,
      20,
      21,
      24,
      25,
      26,
      29,
      30,
      31,
      32,
      33,
      34,
      35,
      36,
      37,
      38,
      39,
      40,
      41,
      42,
      43,
      44,
      45,
      46,
      47,
      48,
      49,
      50,
      51,
      52,
      53,
      54,
      55,
      56,
      57,
      58,
      59,
      60,
      61,
      62,
      63,
      64,
      68,
      69,
      70,
      71,
      72,
      73,
      74,
      75,
      76,
      77,
      78,
      79,
      80,
      81,
      82,
      83,
      84,
      85,
      86,
      87,
      -127,
      -126,
      -125,
      -124,
      88,
      89,
      90,
      91,
      92,
      94,
      95,
      96,
      97,
      98,
      99,
      100,
      101,
      102,
      103,
      104,
      105,
      106,
      107,
      108,
      109,
      110,
      111,
      112,
      113,
      114,
      115,
      116,
      117,
      118,
      119,
      120,
      121,
      122,
      123,
      124,
      125,
      127,
      -128,
      BLOCK_COAL,
      BLOCK_IRON,
      BLOCK_GOLD,
      BLOCK_REDSTONE,
      BLOCK_EMERALD,
      BLOCK_LAPIS,
      BLOCK_DIAMOND,
      BLOCK_QUARTZ,
      STAIRS_WOOD,
      STAIRS_COBBLE,
      FENCE,
      BOOKSHELF,
      TORCH,
      IRON_BARS,
      GLASS_PANE,
      SLAB_COBBLE,
      SLAB_OAK,
      WOOL_WHITE,
      WOOL_ORANGE,
      WOOL_MAGENTA,
      WOOL_LIGHT_BLUE,
      WOOL_YELLOW,
      WOOL_LIME,
      WOOL_PINK,
      WOOL_GRAY,
      WOOL_LIGHT_GRAY,
      WOOL_CYAN,
      WOOL_PURPLE,
      WOOL_BLUE,
      WOOL_BROWN,
      WOOL_GREEN,
      WOOL_RED,
      WOOL_BLACK,
      CARPET_WHITE,
      CARPET_ORANGE,
      CARPET_MAGENTA,
      CARPET_LIGHT_BLUE,
      CARPET_YELLOW,
      CARPET_LIME,
      CARPET_PINK,
      CARPET_GRAY,
      CARPET_LIGHT_GRAY,
      CARPET_CYAN,
      CARPET_PURPLE,
      CARPET_BLUE,
      CARPET_BROWN,
      CARPET_GREEN,
      CARPET_RED,
      CARPET_BLACK,
      PLATE_STONE,
      PLATE_OAK,
      PLATE_GOLD,
      PLATE_IRON,
      LADDER,
      BED_BLOCK
    };
    private int libScroll = 0;
    private Vector primedTNT = new Vector();
    private static final int TNT_FUSE_MS = 4000;
    private static final int TNT_BLINK_PERIOD_MS = 400;

    private class PrimedTNT {
      int x, y, z;
      int t;

      PrimedTNT(int x, int y, int z, int t) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.t = t;
      }
    }

    private void markChunkDirtyAt(int x, int z) {
      int cx = x / CHUNK_SIZE, cz = z / CHUNK_SIZE;
      if (cx >= 0 && cx < CHUNKS_X && cz >= 0 && cz < CHUNKS_Z) {
        chunks[cx + cz * CHUNKS_X].dirty = true;
        if (x % CHUNK_SIZE == 0 && cx > 0) chunks[(cx - 1) + cz * CHUNKS_X].dirty = true;
        if (x % CHUNK_SIZE == 15 && cx < CHUNKS_X - 1)
          chunks[(cx + 1) + cz * CHUNKS_X].dirty = true;
        if (z % CHUNK_SIZE == 0 && cz > 0) chunks[cx + (cz - 1) * CHUNKS_X].dirty = true;
        if (z % CHUNK_SIZE == 15 && cz < CHUNKS_Z - 1)
          chunks[cx + (cz + 1) * CHUNKS_X].dirty = true;
      }
    }

    private void primeTNT(int x, int y, int z) {
      if (getBlock(x, y, z) != TNT) return;
      for (int i = 0; i < primedTNT.size(); i++) {
        PrimedTNT p = (PrimedTNT) primedTNT.elementAt(i);
        if (p.x == x && p.y == y && p.z == z) return;
      }
      primedTNT.addElement(new PrimedTNT(x, y, z, TNT_FUSE_MS));
      setData(x, y, z, 255);
      markChunkDirtyAt(x, z);
    }

    private void explodeTNT(int x, int y, int z) {
      if (getBlock(x, y, z) == TNT) setBlockAndDirty(x, y, z, AIR);
      int r = 3;
      int r2 = r * r;
      for (int dy = -r; dy <= r; dy++) {
        for (int dz = -r; dz <= r; dz++) {
          for (int dx = -r; dx <= r; dx++) {
            if (dx * dx + dy * dy + dz * dz > r2) continue;
            int nx = x + dx, ny = y + dy, nz = z + dz;
            byte b = getBlock(nx, ny, nz);
            if (b == AIR) continue;
            if (b == BEDROCK) continue;
            setBlockAndDirty(nx, ny, nz, AIR);
            if (setDrops == 1) {
              byte drop = getDropItem(b, (byte) 0);
              if (drop != AIR) {
                drops.addElement(new Drop(nx + 0.5f, ny + 0.5f, nz + 0.5f, drop, 0, 0, 0, 1, 500));
              }
            }
          }
        }
      }
    }

    private void updatePrimedTNT(int dt) {
      for (int i = primedTNT.size() - 1; i >= 0; i--) {
        PrimedTNT p = (PrimedTNT) primedTNT.elementAt(i);
        p.t -= dt;
        if (p.t < 0) p.t = 0;
        int d = (p.t * 255) / TNT_FUSE_MS;
        setData(p.x, p.y, p.z, d);
        markChunkDirtyAt(p.x, p.z);
        if (p.t == 0) {
          setData(p.x, p.y, p.z, 0);
          explodeTNT(p.x, p.y, p.z);
          primedTNT.removeElementAt(i);
        }
      }
    }

    private void getCol(byte id, byte[] rgb) {
      int c = getBlockColor(id);
      rgb[0] = (byte) ((c >> 16) & 0xFF);
      rgb[1] = (byte) ((c >> 8) & 0xFF);
      rgb[2] = (byte) (c & 0xFF);
    }

    private int getBlockColorAt(int x, int y, int z, byte id) {
      int c = getBlockColor(id);
      if (id != TNT) return c;
      int d = getData(x, y, z) & 0xFF;
      if (d <= 0) return c;
      long now = System.currentTimeMillis();
      double ph = (now % TNT_BLINK_PERIOD_MS) / (double) TNT_BLINK_PERIOD_MS;
      double osc = 0.5 - 0.5 * Math.cos(ph * 2.0 * Math.PI);
      double prog = 1.0 - (d / 255.0);
      double mix = (0.25 + 0.75 * prog) * osc;
      if (mix > 1.0) mix = 1.0;
      int r = (c >> 16) & 255;
      int g = (c >> 8) & 255;
      int b = (c) & 255;
      r = (int) (r + (255 - r) * mix);
      g = (int) (g + (255 - g) * mix);
      b = (int) (b + (255 - b) * mix);
      return (r << 16) | (g << 8) | b;
    }

    private int getLight(int x, int y, int z) {
      if (y >= WORLD_H) return (int) (15 * dayLightFactor);
      if (x < 0 || x >= WORLD_X || z < 0 || z >= WORLD_Y || y < 0) return 0;
      if (mapLight == null) return (int) (15 * dayLightFactor);
      int val = mapLight[x + z * WORLD_X + y * (WORLD_X * WORLD_Y)] & 0xFF;
      int sky = (val >> 4) & 0xF;
      int blk = val & 0xF;
      int effSky = (int) (sky * dayLightFactor);
      return Math.max(effSky, blk);
    }

    private int getEmission(byte b) {
      if (b == TORCH) return 14;
      if (b == LAVA || b == LAVA_FLOW) return 15;
      if (b == FIRE) return 15;
      if (b == GLOWSTONE) return 15;
      if (b == JACK_O_LANTERN) return 15;
      if (b == PORTAL) return 11;
      if (b == GLOWSTONE_DUST) return 10;
      if (b == ORE_REDSTONE) return 9;
      if (b == FURNACE) return 13;
      return 0;
    }

    private boolean isTransp(int x, int y, int z) {
      byte b = getBlock(x, y, z);
      if (b >= CARPET_BLACK && b <= CARPET_WHITE) return true;
      if (b == LADDER) return true;
      if (b == LEAVES
          || b == LEAVES_BIRCH
          || b == LEAVES_SPRUCE
          || b == LEAVES_JUNGLE
          || b == LEAVES_ACACIA
          || b == LEAVES_DARK_OAK) return true;
      return b == AIR
          || b == GLASS
          || b == WEB
          || isWater(b)
          || isCrossed(b)
          || b == ICE
          || b == BARRIER
          || b == WOOD_DOOR
          || b == PLANT_TALL_GRASS
          || b == IRON_BARS
          || b == SLAB_COBBLE
          || b == SLAB_OAK;
    }

    private void checkLightInit() {
      if (!lightInit && world != null) {
        initLightRamp();
        initLighting();
        if (chunks != null) {
          for (int i = 0; i < chunks.length; i++) {
            if (chunks[i] != null) chunks[i].dirty = true;
          }
        }
      }
    }

    private void initLighting() {
      if (world == null) return;
      if (mapLight == null || mapLight.length != world.length) mapLight = new byte[world.length];
      for (int i = 0; i < mapLight.length; i++) mapLight[i] = 0;
      for (int x = 0; x < WORLD_X; x++) {
        for (int z = 0; z < WORLD_Y; z++) {
          boolean sky = true;
          for (int y = WORLD_H - 1; y >= 0; y--) {
            int idx = x + z * WORLD_X + y * (WORLD_X * WORLD_Y);
            byte b = world[idx];
            int emit = getEmission(b);
            if (sky) {
              mapLight[idx] = (byte) (0xF0 | emit);
              if (!isTransp(x, y, z)) sky = false;
            } else {
              mapLight[idx] = (byte) emit;
            }
          }
        }
      }
      spreadLightIterative();
      lightInit = true;
    }

    private void updateLightAt(int x, int y, int z) {
      int rad = 8;
      int minX = Math.max(0, x - rad), maxX = Math.min(WORLD_X - 1, x + rad);
      int minZ = Math.max(0, z - rad), maxZ = Math.min(WORLD_Y - 1, z + rad);
      int minY = 0;
      int maxY = Math.min(WORLD_H - 1, y + rad);
      for (int ix = minX; ix <= maxX; ix++) {
        for (int iz = minZ; iz <= maxZ; iz++) {
          boolean sky = true;
          for (int iy = WORLD_H - 1; iy >= minY; iy--) {
            int idx = ix + iz * WORLD_X + iy * (WORLD_X * WORLD_Y);
            if (sky) {
              int emit = getEmission(world[idx]);
              mapLight[idx] = (byte) (0xF0 | emit);
              if (!isTransp(ix, iy, iz)) sky = false;
            } else {
              int emit = getEmission(world[idx]);
              mapLight[idx] = (byte) emit;
            }
          }
        }
      }
      spreadLightRegion(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private void spreadLightRegion(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
      boolean changed = true;
      int pass = 0;
      while (changed && pass < 24) {
        changed = false;
        for (int x = minX; x <= maxX; x++) {
          for (int z = minZ; z <= maxZ; z++) {
            for (int y = maxY; y >= minY; y--) {
              int idx = x + z * WORLD_X + y * (WORLD_X * WORLD_Y);
              int cur = mapLight[idx] & 0xFF;
              int curS = (cur >> 4) & 0xF;
              int curB = cur & 0xF;
              if (curS == 15 && curB == 15) continue;
              int maxS = 0, maxB = 0;
              if (x > 0) {
                int v = mapLight[idx - 1] & 0xFF;
                maxS = Math.max(maxS, (v >> 4) & 0xF);
                maxB = Math.max(maxB, v & 0xF);
              }
              if (x < WORLD_X - 1) {
                int v = mapLight[idx + 1] & 0xFF;
                maxS = Math.max(maxS, (v >> 4) & 0xF);
                maxB = Math.max(maxB, v & 0xF);
              }
              if (y > 0) {
                int v = mapLight[idx - WORLD_X * WORLD_Y] & 0xFF;
                maxS = Math.max(maxS, (v >> 4) & 0xF);
                maxB = Math.max(maxB, v & 0xF);
              }
              if (y < WORLD_H - 1) {
                int v = mapLight[idx + WORLD_X * WORLD_Y] & 0xFF;
                maxS = Math.max(maxS, (v >> 4) & 0xF);
                maxB = Math.max(maxB, v & 0xF);
              }
              if (z > 0) {
                int v = mapLight[idx - WORLD_X] & 0xFF;
                maxS = Math.max(maxS, (v >> 4) & 0xF);
                maxB = Math.max(maxB, v & 0xF);
              }
              if (z < WORLD_Y - 1) {
                int v = mapLight[idx + WORLD_X] & 0xFF;
                maxS = Math.max(maxS, (v >> 4) & 0xF);
                maxB = Math.max(maxB, v & 0xF);
              }
              int newS = maxS - 1;
              int newB = maxB - 1;
              byte b = world[idx];
              if (isWater(b) || b == ICE) {
                newS -= 2;
                newB -= 2;
              }
              if (!isTransp(x, y, z)) {
                newS -= 1;
                newB -= 1;
              }
              if (newS < 0) newS = 0;
              if (newB < 0) newB = 0;
              if (newS > curS || newB > curB) {
                mapLight[idx] = (byte) ((Math.max(curS, newS) << 4) | Math.max(curB, newB));
                changed = true;
              }
            }
          }
        }
        pass++;
      }
    }

    private void spreadLightIterative() {
      spreadLightRegion(0, 0, 0, WORLD_X - 1, WORLD_H - 1, WORLD_Y - 1);
    }

    private void applyBlockLight(int x, int y, int z, int startVI, int vI) {
      byte myBlock = getBlock(x, y, z);
      if (getEmission(myBlock) > 0) {
        for (int k = startVI; k < vI; k++) tmpCols[k] = (byte) 255;
        return;
      }
      boolean hasTexture = (getTexByBlockId(myBlock) != null);
      if (setLight == 0) {
        if (hasTexture) {
          for (int k = startVI; k < vI; k++) tmpCols[k] = (byte) 255;
        }
        return;
      }
      int bl = getLight(x, y, z);
      if (setLight == 1) {
        int lVal = lightRamp[bl];
        byte c = (byte) lVal;
        for (int k = startVI; k < vI; k++) {
          if (hasTexture) {
            tmpCols[k] = c;
          } else {
            int r = tmpCols[k] & 0xFF;
            tmpCols[k] = (byte) ((r * lVal) / 255);
          }
        }
        return;
      }
      for (int k = startVI; k < vI; k += 3) {
        short vx = tmpVerts[(k / 3) * 3];
        short vy = tmpVerts[(k / 3) * 3 + 1];
        short vz = tmpVerts[(k / 3) * 3 + 2];
        int ox = (vx > x * V_SCALE + V_SCALE / 2) ? 1 : -1;
        int oy = (vy > y * V_SCALE + V_SCALE / 2) ? 1 : -1;
        int oz = (vz > z * V_SCALE + V_SCALE / 2) ? 1 : -1;
        int l0 = bl;
        int l1 = getLight(x + ox, y, z);
        int l2 = getLight(x, y, z + oz);
        int l3 = getLight(x + ox, y, z + oz);
        int l4 = getLight(x, y + oy, z);
        int total = l0 + l1 + l2 + l3 + l4;
        if (l0 > 5 && total < 15) total = l0 * 5;
        int avg = total / 5;
        if (avg > 15) avg = 15;
        int lVal = lightRamp[avg];
        byte c = (byte) lVal;
        if (hasTexture) {
          tmpCols[k] = c;
          tmpCols[k + 1] = c;
          tmpCols[k + 2] = c;
        } else {
          int r = tmpCols[k] & 0xFF;
          tmpCols[k] = (byte) ((r * lVal) / 255);
          int g = tmpCols[k + 1] & 0xFF;
          tmpCols[k + 1] = (byte) ((g * lVal) / 255);
          int b = tmpCols[k + 2] & 0xFF;
          tmpCols[k + 2] = (byte) ((b * lVal) / 255);
        }
      }
    }

    class Chunk {
      int cx, cz;
      Mesh mesh;
      boolean dirty = true, empty = true;

      public Chunk(int cx, int cz) {
        this.cx = cx;
        this.cz = cz;
      }

      public void rebuild() {
        Hashtable buckets = new Hashtable();
        int sx = cx * CHUNK_SIZE,
            sz = cz * CHUNK_SIZE,
            vI = 0,
            iI = 0,
            vc = 0,
            lim = tmpVerts.length - 24,
            wO = sx + sz * WORLD_X;
        for (int x = sx; x < sx + CHUNK_SIZE; x++) {
          int cI = x + sz * WORLD_X;
          for (int z = sz; z < sz + CHUNK_SIZE; z++) {
            int idx = cI;
            for (int y = 0; y < WORLD_H; y++) {
              if (vI >= lim) break;
              byte b = world[idx];
              if (b != AIR) {
                boolean complex =
                    isCrossed(b)
                        || b == LADDER
                        || b == PORTAL
                        || b == REDSTONE
                        || b == WOOD_DOOR
                        || b == NETHER_FENCE
                        || b == WHEAT_BLOCK
                        || b == FENCE
                        || b == IRON_BARS
                        || b == GLASS_PANE;
                if (complex) {
                  String texKey = "def";
                  String name = getItemName(b).replace(' ', '_').toLowerCase();
                  if (b == PLANT_TALL_GRASS) {
                    name = (getData(x, y, z) == 0) ? "tall_grass_bottom" : "tall_grass_top";
                  }
                  if (b == PORTAL && setAnimations == 1) name = "portal_anim";
                  if (b == 65) name = "fire";
                  if (b == FARMLAND && getData(x, y, z) > 0) name = "farmland_wet";
                  if (b == WHEAT_BLOCK) name = "wheat_block_" + getData(x, y, z);
                  if (b == GLASS_PANE && getTex("glass_pane") == null) name = "glass";
                  if (getTex(name) != null) texKey = name;
                  int startII = iI;
                  int startVI = vI;
                  if (isCrossed(b)) {
                    if (b == TORCH) addTorch(x, y, z, getData(x, y, z), vI);
                    else ac(x, y, z, vI);
                    iI += 12;
                    vI += 24;
                    vc += 8;
                  } else if (b == PORTAL) {
                    ap(x, y, z, getData(x, y, z), vI);
                    iI += 6;
                    vI += 12;
                    vc += 4;
                  } else if (b == IRON_BARS || b == GLASS_PANE) {
                    int d = worldData[idx] & 3;
                    int ax = (d == 1 || d == 3) ? 2 : 1;
                    ap(x, y, z, ax, vI);
                    iI += 6;
                    vI += 12;
                    vc += 4;
                  } else if (b == LADDER) {
                    addLadder(x, y, z, getData(x, y, z), vI);
                    iI += 12;
                    vI += 24;
                    vc += 8;
                  } else if (b == REDSTONE) {
                    af(x, y, z, 0, vI, (byte) 255, (byte) 0, (byte) 0, 2, 2, 2, 2, 0);
                    iI += 6;
                    vI += 12;
                    vc += 4;
                  } else if (b == WOOD_DOOR) {
                    int d = getData(x, y, z) & 0xFF;
                    int c = getBlockColorAt(x, y, z, b);
                    byte R = (byte) ((c >> 16) & 0xFF),
                        G = (byte) ((c >> 8) & 0xFF),
                        B = (byte) (c & 0xFF);
                    addDoorBox(x, y, z, d, vI, R, G, B);
                    iI += 36;
                    vI += 72;
                    vc += 24;
                  } else if (b == NETHER_FENCE || b == FENCE) {
                    byte[] rgb = new byte[3];
                    getCol(b, rgb);
                    addFenceBox(x, y, z, vI, rgb[0], rgb[1], rgb[2]);
                    iI += 36;
                    vI += 72;
                    vc += 24;
                  } else if (b == WHEAT_BLOCK) {
                    int age = getData(x, y, z);
                    int h = (int) ((age + 1) * V_SCALE / 8.0f);
                    int c = getBlockColorAt(x, y, z, b);
                    byte R = (byte) ((c >> 16) & 0xFF),
                        G = (byte) ((c >> 8) & 0xFF),
                        B = (byte) (c & 0xFF);
                    af(x, y, z, 2, vI, R, G, B, h, h, h, h, 0);
                    vI += 12;
                    iI += 6;
                    vc += 4;
                    af(x, y, z, 3, vI, R, G, B, h, h, h, h, 0);
                    vI += 12;
                    iI += 6;
                    vc += 4;
                    af(x, y, z, 4, vI, R, G, B, h, h, h, h, 0);
                    vI += 12;
                    iI += 6;
                    vc += 4;
                    af(x, y, z, 5, vI, R, G, B, h, h, h, h, 0);
                    vI += 12;
                    iI += 6;
                    vc += 4;
                  }
                  applyBlockLight(x, y, z, startVI, vI);
                  int numAdded = iI - startII;
                  if (numAdded > 0) {
                    Vector v = (Vector) buckets.get(texKey);
                    if (v == null) {
                      v = new Vector();
                      buckets.put(texKey, v);
                    }
                    for (int k = 0; k < numAdded; k++)
                      v.addElement(new Integer(tmpIndices[startII + k]));
                  }
                } else if (b != BARRIER) {
                  boolean isW = (b == WATER || b == WATER_FLOW),
                      isL = (b == LAVA || b == LAVA_FLOW);
                  int h00 = V_SCALE, h10 = V_SCALE, h11 = V_SCALE, h01 = V_SCALE;
                  int yOff = 0;
                  if (b == SLAB_COBBLE || b == SLAB_OAK || b == REDSTONE) {
                    int d = getData(x, y, z);
                    if (d != 2) {
                      h00 = h10 = h11 = h01 = V_SCALE / 2;
                      if (d == 1) yOff = (V_SCALE / 2);
                    }
                  }
                  if (b == PLATE_STONE || b == PLATE_OAK || b == PLATE_GOLD || b == PLATE_IRON) {
                    int d = getData(x, y, z);
                    int hVal = (d == 1) ? 1 : 2;
                    h00 = h10 = h11 = h01 = hVal;
                  }
                  if (b >= CARPET_BLACK && b <= CARPET_WHITE) {
                    h00 = h10 = h11 = h01 = 2;
                  }
                  if (isW || isL) {
                    if (b == WATER || b == LAVA) {
                      h00 = h10 = h11 = h01 = V_SCALE;
                    } else {
                      h00 = getCH(x, y, z);
                      h10 = getCH(x + 1, y, z);
                      h11 = getCH(x + 1, y, z + 1);
                      h01 = getCH(x, y, z + 1);
                    }
                  }
                  int c = getBlockColorAt(x, y, z, b);
                  byte R = (byte) ((c >> 16) & 0xFF),
                      G = (byte) ((c >> 8) & 0xFF),
                      B = (byte) (c & 0xFF);
                  if (sd(x, y + 1, z, b)) {
                    int sII = iI;
                    int sVI = vI;
                    af(
                        x,
                        y,
                        z,
                        0,
                        vI,
                        R,
                        G,
                        B,
                        (b == GRASS_PATH ? (V_SCALE * 15) / 16 : h00),
                        (b == GRASS_PATH ? (V_SCALE * 15) / 16 : h10),
                        (b == GRASS_PATH ? (V_SCALE * 15) / 16 : h11),
                        (b == GRASS_PATH ? (V_SCALE * 15) / 16 : h01),
                        yOff);
                    iI += 6;
                    vI += 12;
                    vc += 4;
                    applyBlockLight(x, y, z, sVI, vI);
                    String tk =
                        MinecraftMIDlet.this.canvas.getTexName(
                            b, MinecraftMIDlet.this.canvas.getRotatedFace(0, getData(x, y, z), b));
                    if (b == TNT
                        && (getData(x, y, z) & 0xFF) > 0
                        && ((System.currentTimeMillis() % 400) > 200)) tk = "flash";
                    Vector v = (Vector) buckets.get(tk);
                    if (v == null) {
                      v = new Vector();
                      buckets.put(tk, v);
                    }
                    for (int k = 0; k < (iI - sII); k++)
                      v.addElement(new Integer(tmpIndices[sII + k]));
                  }
                  if (sd(x, y - 1, z, b)) {
                    int sII = iI;
                    int sVI = vI;
                    af(x, y, z, 1, vI, R, G, B, h00, h10, h11, h01, yOff);
                    iI += 6;
                    vI += 12;
                    vc += 4;
                    applyBlockLight(x, y, z, sVI, vI);
                    String tk =
                        MinecraftMIDlet.this.canvas.getTexName(
                            b, MinecraftMIDlet.this.canvas.getRotatedFace(1, getData(x, y, z), b));
                    if (b == TNT
                        && (getData(x, y, z) & 0xFF) > 0
                        && ((System.currentTimeMillis() % 400) > 200)) tk = "flash";
                    Vector v = (Vector) buckets.get(tk);
                    if (v == null) {
                      v = new Vector();
                      buckets.put(tk, v);
                    }
                    for (int k = 0; k < (iI - sII); k++)
                      v.addElement(new Integer(tmpIndices[sII + k]));
                  }
                  if (sd(x, y, z + 1, b)) {
                    int sII = iI;
                    int sVI = vI;
                    af(x, y, z, 2, vI, R, G, B, h00, h10, h11, h01, yOff);
                    iI += 6;
                    vI += 12;
                    vc += 4;
                    applyBlockLight(x, y, z, sVI, vI);
                    String tk =
                        MinecraftMIDlet.this.canvas.getTexName(
                            b, MinecraftMIDlet.this.canvas.getRotatedFace(2, getData(x, y, z), b));
                    if (b == TNT
                        && (getData(x, y, z) & 0xFF) > 0
                        && ((System.currentTimeMillis() % 400) > 200)) tk = "flash";
                    Vector v = (Vector) buckets.get(tk);
                    if (v == null) {
                      v = new Vector();
                      buckets.put(tk, v);
                    }
                    for (int k = 0; k < (iI - sII); k++)
                      v.addElement(new Integer(tmpIndices[sII + k]));
                  }
                  if (sd(x, y, z - 1, b)) {
                    int sII = iI;
                    int sVI = vI;
                    af(x, y, z, 3, vI, R, G, B, h00, h10, h11, h01, yOff);
                    iI += 6;
                    vI += 12;
                    vc += 4;
                    applyBlockLight(x, y, z, sVI, vI);
                    String tk =
                        MinecraftMIDlet.this.canvas.getTexName(
                            b, MinecraftMIDlet.this.canvas.getRotatedFace(3, getData(x, y, z), b));
                    if (b == TNT
                        && (getData(x, y, z) & 0xFF) > 0
                        && ((System.currentTimeMillis() % 400) > 200)) tk = "flash";
                    Vector v = (Vector) buckets.get(tk);
                    if (v == null) {
                      v = new Vector();
                      buckets.put(tk, v);
                    }
                    for (int k = 0; k < (iI - sII); k++)
                      v.addElement(new Integer(tmpIndices[sII + k]));
                  }
                  if (sd(x + 1, y, z, b)) {
                    int sII = iI;
                    int sVI = vI;
                    af(x, y, z, 4, vI, R, G, B, h00, h10, h11, h01, yOff);
                    iI += 6;
                    vI += 12;
                    vc += 4;
                    applyBlockLight(x, y, z, sVI, vI);
                    String tk =
                        MinecraftMIDlet.this.canvas.getTexName(
                            b, MinecraftMIDlet.this.canvas.getRotatedFace(4, getData(x, y, z), b));
                    if (b == TNT
                        && (getData(x, y, z) & 0xFF) > 0
                        && ((System.currentTimeMillis() % 400) > 200)) tk = "flash";
                    Vector v = (Vector) buckets.get(tk);
                    if (v == null) {
                      v = new Vector();
                      buckets.put(tk, v);
                    }
                    for (int k = 0; k < (iI - sII); k++)
                      v.addElement(new Integer(tmpIndices[sII + k]));
                  }
                  if (sd(x - 1, y, z, b)) {
                    int sII = iI;
                    int sVI = vI;
                    af(x, y, z, 5, vI, R, G, B, h00, h10, h11, h01, yOff);
                    iI += 6;
                    vI += 12;
                    vc += 4;
                    applyBlockLight(x, y, z, sVI, vI);
                    String tk =
                        MinecraftMIDlet.this.canvas.getTexName(
                            b, MinecraftMIDlet.this.canvas.getRotatedFace(5, getData(x, y, z), b));
                    if (b == TNT
                        && (getData(x, y, z) & 0xFF) > 0
                        && ((System.currentTimeMillis() % 400) > 200)) tk = "flash";
                    Vector v = (Vector) buckets.get(tk);
                    if (v == null) {
                      v = new Vector();
                      buckets.put(tk, v);
                    }
                    for (int k = 0; k < (iI - sII); k++)
                      v.addElement(new Integer(tmpIndices[sII + k]));
                  }
                }
              }
              idx += WORLD_X * WORLD_Y;
            }
            cI += WORLD_X;
          }
        }
        if (vc > 0) {
          empty = false;
          VertexArray vp = new VertexArray(vc, 3, 2);
          vp.set(0, vc, tmpVerts);
          VertexArray vt = new VertexArray(vc, 2, 2);
          vt.set(0, vc, tmpTexs);
          VertexArray vcl = new VertexArray(vc, 3, 1);
          vcl.set(0, vc, tmpCols);
          VertexBuffer vb = new VertexBuffer();
          vb.setPositions(vp, 1.0f / V_SCALE, null);
          vb.setTexCoords(0, vt, 1.0f, null);
          vb.setColors(vcl);
          vb.setDefaultColor(0xFFFFFFFF);
          int numSubs = buckets.size();
          IndexBuffer[] ibs = new IndexBuffer[numSubs];
          Appearance[] apps = new Appearance[numSubs];
          Vector keysVec = new Vector();
          Enumeration ke = buckets.keys();
          while (ke.hasMoreElements()) keysVec.addElement(ke.nextElement());
          if (keysVec.contains("def")) {
            keysVec.removeElement("def");
            keysVec.insertElementAt("def", 0);
          }
          for (int i = 1; i < keysVec.size(); i++) {
            for (int j = 1; j < keysVec.size() - 1; j++) {
              String k1 = (String) keysVec.elementAt(j);
              String k2 = (String) keysVec.elementAt(j + 1);
              boolean t1 = (k1.indexOf("water") >= 0 || k1.indexOf("ice") >= 0);
              if (t1) {
                keysVec.setElementAt(k2, j);
                keysVec.setElementAt(k1, j + 1);
              }
            }
          }
          int bIdx = 0;
          for (int ki = 0; ki < keysVec.size(); ki++) {
            String key = (String) keysVec.elementAt(ki);
            Vector v = (Vector) buckets.get(key);
            int[] ind = new int[v.size()];
            for (int i = 0; i < v.size(); i++) ind[i] = ((Integer) v.elementAt(i)).intValue();
            int[] lens = new int[ind.length / 3];
            for (int i = 0; i < lens.length; i++) lens[i] = 3;
            ibs[bIdx] = new TriangleStripArray(ind, lens);
            if (key.equals("def")) {
              apps[bIdx] = appWorld;
            } else {
              Texture2D t = MinecraftMIDlet.this.canvas.getTex(key);
              Appearance app = new Appearance();
              app.setTexture(0, t);
              app.setMaterial(matMain);
              boolean isCutout =
                  (key.indexOf("wheat") >= 0
                      || key.indexOf("leaves") >= 0
                      || key.indexOf("glass") >= 0
                      || key.indexOf("mushroom") >= 0
                      || key.indexOf("flower") >= 0
                      || key.indexOf("grass") >= 0
                      || key.indexOf("bush") >= 0
                      || key.indexOf("sapling") >= 0
                      || key.indexOf("torch") >= 0
                      || key.indexOf("fire") >= 0
                      || key.indexOf("portal") >= 0
                      || key.equals("cobweb")
                      || key.equals("nether_wart")
                      || key.equals("cactus")
                      || key.indexOf("reeds") >= 0
                      || key.indexOf("dandelion") >= 0
                      || key.indexOf("rose") >= 0
                      || key.indexOf("redstone") >= 0
                      || key.indexOf("iron_bars") >= 0
                      || key.equals("ladder"));
              boolean isIce = (key.indexOf("ice") >= 0);
              boolean isWater = (key.indexOf("water") >= 0);
              if (isIce) {
                PolygonMode pm = new PolygonMode();
                pm.setCulling(PolygonMode.CULL_BACK);
                pm.setShading(PolygonMode.SHADE_FLAT);
                app.setPolygonMode(pm);
                CompositingMode cm = new CompositingMode();
                cm.setBlending(CompositingMode.ALPHA);
                cm.setDepthWriteEnable(true);
                cm.setDepthTestEnable(true);
                app.setCompositingMode(cm);
              } else if (isWater) {
                PolygonMode pm = new PolygonMode();
                pm.setCulling(PolygonMode.CULL_NONE);
                pm.setShading(PolygonMode.SHADE_FLAT);
                app.setPolygonMode(pm);
                CompositingMode cm = new CompositingMode();
                cm.setBlending(CompositingMode.ALPHA);
                cm.setDepthWriteEnable(false);
                cm.setDepthTestEnable(true);
                app.setCompositingMode(cm);
              } else {
                app.setPolygonMode(appWorld.getPolygonMode());
                if (isCutout) {
                  CompositingMode cm = new CompositingMode();
                  if (key.indexOf("glass") >= 0) {
                    cm.setBlending(CompositingMode.ALPHA);
                    cm.setDepthWriteEnable(key.indexOf("pane") == -1);
                  } else {
                    cm.setAlphaThreshold(0.5f);
                    cm.setBlending(CompositingMode.ALPHA);
                  }
                  app.setCompositingMode(cm);
                }
              }
              apps[bIdx] = app;
            }
            bIdx++;
          }
          mesh = new Mesh(vb, ibs, apps);
        } else {
          empty = true;
          mesh = null;
        }
        dirty = false;
      }

      private void addTorch(int x, int y, int z, int d, int o) {
        if (d == 0 || d == 5) {
          ac(x, y, z, o);
          return;
        }
        short X = (short) (x * V_SCALE), Y = (short) (y * V_SCALE), Z = (short) (z * V_SCALE);
        short S = (short) V_SCALE;
        short H = (short) V_SCALE;
        int tilt = V_SCALE / 3;
        int wallOff = 8;
        short xb0 = X, zb0 = Z, xb1 = (short) (X + S), zb1 = (short) (Z + S);
        short xt0 = X, zt0 = Z, xt1 = (short) (X + S), zt1 = (short) (Z + S);
        short yb = Y, yt = (short) (Y + H);
        if (d == 1) {
          xb0 -= wallOff;
          xb1 -= wallOff;
          xt0 += tilt;
          xt1 += tilt;
          yb += tilt;
          yt += tilt;
        } else if (d == 2) {
          xb0 += wallOff;
          xb1 += wallOff;
          xt0 -= tilt;
          xt1 -= tilt;
          yb += tilt;
          yt += tilt;
        } else if (d == 3) {
          zb0 -= wallOff;
          zb1 -= wallOff;
          zt0 += tilt;
          zt1 += tilt;
          yb += tilt;
          yt += tilt;
        } else if (d == 4) {
          zb0 += wallOff;
          zb1 += wallOff;
          zt0 -= tilt;
          zt1 -= tilt;
          yb += tilt;
          yt += tilt;
        }
        addQuad(
            xb0, yb, zb0, xb1, yb, zb1, xt1, yt, zt1, xt0, yt, zt0, o, (byte) 255, (byte) 255,
            (byte) 255);
        int t = o / 3 * 2;
        tmpTexs[t + 1] = 1;
        tmpTexs[t + 3] = 1;
        tmpTexs[t + 5] = 0;
        tmpTexs[t + 7] = 0;
        addQuad(
            xb0, yb, zb1, xb1, yb, zb0, xt1, yt, zt0, xt0, yt, zt1, o + 12, (byte) 255, (byte) 255,
            (byte) 255);
        t = (o + 12) / 3 * 2;
        tmpTexs[t + 1] = 1;
        tmpTexs[t + 3] = 1;
        tmpTexs[t + 5] = 0;
        tmpTexs[t + 7] = 0;
      }

      private void ac(int x, int y, int z, int o) {
        short X = (short) (x * V_SCALE),
            Y = (short) (y * V_SCALE),
            Z = (short) (z * V_SCALE),
            S = (short) V_SCALE;
        short x0 = X,
            y0 = Y,
            z0 = Z,
            x1 = (short) (X + S),
            y1 = (short) (Y + S),
            z1 = (short) (Z + S);
        addQuad(
            x0, y0, z0, x1, y0, z1, x1, y1, z1, x0, y1, z0, o, (byte) 255, (byte) 255, (byte) 255);
        int t = o / 3 * 2;
        tmpTexs[t + 1] = 1;
        tmpTexs[t + 3] = 1;
        tmpTexs[t + 5] = 0;
        tmpTexs[t + 7] = 0;
        addQuad(
            x0, y0, z1, x1, y0, z0, x1, y1, z0, x0, y1, z1, o + 12, (byte) 255, (byte) 255,
            (byte) 255);
        t = (o + 12) / 3 * 2;
        tmpTexs[t + 1] = 1;
        tmpTexs[t + 3] = 1;
        tmpTexs[t + 5] = 0;
        tmpTexs[t + 7] = 0;
      }

      private void ap(int x, int y, int z, int axis, int o) {
        short X = (short) (x * V_SCALE),
            Y = (short) (y * V_SCALE),
            Z = (short) (z * V_SCALE),
            S = (short) V_SCALE,
            H = (short) V_SCALE;
        short x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3;
        short mid = (short) (V_SCALE / 2);
        if (axis == 1) {
          x0 = X;
          y0 = Y;
          z0 = (short) (Z + mid);
          x1 = (short) (X + S);
          y1 = Y;
          z1 = (short) (Z + mid);
          x2 = (short) (X + S);
          y2 = (short) (Y + H);
          z2 = (short) (Z + mid);
          x3 = X;
          y3 = (short) (Y + H);
          z3 = (short) (Z + mid);
        } else {
          x0 = (short) (X + mid);
          y0 = Y;
          z0 = Z;
          x1 = (short) (X + mid);
          y1 = Y;
          z1 = (short) (Z + S);
          x2 = (short) (X + mid);
          y2 = (short) (Y + H);
          z2 = (short) (Z + S);
          x3 = (short) (X + mid);
          y3 = (short) (Y + H);
          z3 = Z;
        }
        tmpVerts[o] = x0;
        tmpVerts[o + 1] = y0;
        tmpVerts[o + 2] = z0;
        tmpVerts[o + 3] = x1;
        tmpVerts[o + 4] = y1;
        tmpVerts[o + 5] = z1;
        tmpVerts[o + 6] = x2;
        tmpVerts[o + 7] = y2;
        tmpVerts[o + 8] = z2;
        tmpVerts[o + 9] = x3;
        tmpVerts[o + 10] = y3;
        tmpVerts[o + 11] = z3;
        int t = o / 3 * 2;
        tmpTexs[t] = 0;
        tmpTexs[t + 1] = 0;
        tmpTexs[t + 2] = 1;
        tmpTexs[t + 3] = 0;
        tmpTexs[t + 4] = 1;
        tmpTexs[t + 5] = 1;
        tmpTexs[t + 6] = 0;
        tmpTexs[t + 7] = 1;
        for (int i = 0; i < 4; i++) {
          tmpCols[o + i * 3] = (byte) 80;
          tmpCols[o + i * 3 + 1] = 0;
          tmpCols[o + i * 3 + 2] = (byte) 160;
        }
        int ii = (o / 12) * 6, vs = o / 3;
        tmpIndices[ii] = vs;
        tmpIndices[ii + 1] = vs + 1;
        tmpIndices[ii + 2] = vs + 2;
        tmpIndices[ii + 3] = vs;
        tmpIndices[ii + 4] = vs + 2;
        tmpIndices[ii + 5] = vs + 3;
      }

      private boolean sd(int x, int y, int z, byte s) {
        if (x < 0 || x >= WORLD_X || y < 0 || y >= WORLD_H || z < 0 || z >= WORLD_Y) return true;
        byte n = world[x + z * WORLD_X + y * (WORLD_X * WORLD_Y)];
        if (s == 96 && n == 96) return false;
        if ((s == 22 || s == 23) && n == 96) return false;
        if ((s == 22 || s == 23) && (n == 22 || n == 23)) return false;
        if (n == 0) return true;
        boolean sf = (s == 22 || s == 23 || s == 27 || s == 28);
        boolean nf = (n == 22 || n == 23 || n == 27 || n == 28);
        if (sf && nf) return false;
        if (it(s) && !it(n)) return false;
        if (!it(s) && it(n)) return true;
        return it(n);
      }

      private void addQuad(
          short x0,
          short y0,
          short z0,
          short x1,
          short y1,
          short z1,
          short x2,
          short y2,
          short z2,
          short x3,
          short y3,
          short z3,
          int o,
          byte r,
          byte g,
          byte b) {
        tmpVerts[o] = x0;
        tmpVerts[o + 1] = y0;
        tmpVerts[o + 2] = z0;
        tmpVerts[o + 3] = x1;
        tmpVerts[o + 4] = y1;
        tmpVerts[o + 5] = z1;
        tmpVerts[o + 6] = x2;
        tmpVerts[o + 7] = y2;
        tmpVerts[o + 8] = z2;
        tmpVerts[o + 9] = x3;
        tmpVerts[o + 10] = y3;
        tmpVerts[o + 11] = z3;
        tmpTexs[o / 3 * 2] = 0;
        tmpTexs[o / 3 * 2 + 1] = 0;
        tmpTexs[o / 3 * 2 + 2] = 1;
        tmpTexs[o / 3 * 2 + 3] = 0;
        tmpTexs[o / 3 * 2 + 4] = 1;
        tmpTexs[o / 3 * 2 + 5] = 1;
        tmpTexs[o / 3 * 2 + 6] = 0;
        tmpTexs[o / 3 * 2 + 7] = 1;
        for (int i = 0; i < 4; i++) {
          tmpCols[o + i * 3] = r;
          tmpCols[o + i * 3 + 1] = g;
          tmpCols[o + i * 3 + 2] = b;
        }
        int ii = (o / 12) * 6, vs = o / 3;
        tmpIndices[ii] = vs;
        tmpIndices[ii + 1] = vs + 1;
        tmpIndices[ii + 2] = vs + 2;
        tmpIndices[ii + 3] = vs;
        tmpIndices[ii + 4] = vs + 2;
        tmpIndices[ii + 5] = vs + 3;
      }

      private void addDoorBox(int x, int y, int z, int data, int o, byte r, byte g, byte b) {
        int d = data & 0xFF;
        int dir = d & 3;
        boolean open = (d & 4) != 0;
        short X = (short) (x * V_SCALE), Y = (short) (y * V_SCALE), Z = (short) (z * V_SCALE);
        short S = (short) V_SCALE, H = (short) V_SCALE;
        short T = 3;
        short x0 = X, x1 = (short) (X + S), z0 = Z, z1 = (short) (Z + S);
        if (!open) {
          if ((dir & 1) == 0) {
            if (dir == 0) {
              z0 = (short) (Z + S - T);
            } else {
              z1 = (short) (Z + T);
            }
          } else {
            if (dir == 1) {
              x0 = (short) (X + S - T);
            } else {
              x1 = (short) (X + T);
            }
          }
        } else {
          if ((dir & 1) == 0) {
            if (dir == 0) {
              x1 = (short) (X + T);
            } else {
              x0 = (short) (X + S - T);
            }
          } else {
            if (dir == 1) {
              z0 = (short) (Z + S - T);
            } else {
              z1 = (short) (Z + T);
            }
          }
        }
        short y0 = Y, y1 = (short) (Y + H);
        int p = o;
        addQuad(x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, p, r, g, b);
        p += 12;
        addQuad(x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1, p, r, g, b);
        p += 12;
        addQuad(x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1, p, r, g, b);
        p += 12;
        addQuad(x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, p, r, g, b);
        p += 12;
        addQuad(x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, p, r, g, b);
        p += 12;
        addQuad(x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, p, r, g, b);
      }

      private void addFenceBox(int x, int y, int z, int o, byte r, byte g, byte b) {
        short X = (short) (x * V_SCALE), Y = (short) (y * V_SCALE), Z = (short) (z * V_SCALE);
        short S = (short) V_SCALE;
        short min = (short) (6 * V_SCALE / 16);
        short max = (short) (10 * V_SCALE / 16);
        short x0 = (short) (X + min), x1 = (short) (X + max);
        short z0 = (short) (Z + min), z1 = (short) (Z + max);
        short y0 = Y, y1 = (short) (Y + S);
        int p = o;
        addQuad(x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, p, r, g, b);
        p += 12;
        addQuad(x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1, p, r, g, b);
        p += 12;
        addQuad(x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1, p, r, g, b);
        p += 12;
        addQuad(x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, p, r, g, b);
        p += 12;
        addQuad(x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, p, r, g, b);
        p += 12;
        addQuad(x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, p, r, g, b);
      }

      private void addLadder(int x, int y, int z, int d, int o) {
        short X = (short) (x * V_SCALE), Y = (short) (y * V_SCALE), Z = (short) (z * V_SCALE);
        short S = (short) V_SCALE, H = (short) V_SCALE;
        short off = (short) (V_SCALE / 8);
        short x0 = X,
            y0 = Y,
            z0 = Z,
            x1 = (short) (X + S),
            y1 = (short) (Y + H),
            z1 = (short) (Z + S);
        if (d == 0) {
          z0 = (short) (Z + S - off);
          z1 = z0;
        } else if (d == 1) {
          x0 = (short) (X + off);
          x1 = x0;
        } else if (d == 2) {
          z0 = (short) (Z + off);
          z1 = z0;
        } else if (d == 3) {
          x0 = (short) (X + S - off);
          x1 = x0;
        }
        addQuad(
            x0, y0, z0, x1, y0, z1, x1, y1, z1, x0, y1, z0, o, (byte) 255, (byte) 255, (byte) 255);
        addQuad(
            x1, y0, z1, x0, y0, z0, x0, y1, z0, x1, y1, z1, o + 12, (byte) 255, (byte) 255,
            (byte) 255);
      }

      private void af(
          int x,
          int y,
          int z,
          int f,
          int o,
          byte r,
          byte g,
          byte b,
          int h00,
          int h10,
          int h11,
          int h01,
          int yOff) {
        short X = (short) (x * V_SCALE),
            Y = (short) (y * V_SCALE + yOff),
            Z = (short) (z * V_SCALE),
            S = (short) V_SCALE;
        short x0 = 0,
            y0 = 0,
            z0 = 0,
            x1 = 0,
            y1 = 0,
            z1 = 0,
            x2 = 0,
            y2 = 0,
            z2 = 0,
            x3 = 0,
            y3 = 0,
            z3 = 0;
        short y00 = (short) (Y + h00),
            y10 = (short) (Y + h10),
            y11 = (short) (Y + h11),
            y01 = (short) (Y + h01),
            yb = Y;
        if (f == 0) {
          x0 = X;
          y0 = y01;
          z0 = Z;
          x1 = X;
          y1 = y00;
          z1 = (short) (Z + S);
          x2 = (short) (X + S);
          y2 = y10;
          z2 = (short) (Z + S);
          x3 = (short) (X + S);
          y3 = y11;
          z3 = Z;
        } else if (f == 1) {
          x0 = (short) (X + S);
          y0 = yb;
          z0 = Z;
          x1 = (short) (X + S);
          y1 = yb;
          z1 = (short) (Z + S);
          x2 = X;
          y2 = yb;
          z2 = (short) (Z + S);
          x3 = X;
          y3 = yb;
          z3 = Z;
        } else if (f == 2) {
          x0 = (short) (X + S);
          y0 = y10;
          z0 = (short) (Z + S);
          x1 = X;
          y1 = y00;
          z1 = (short) (Z + S);
          x2 = X;
          y2 = yb;
          z2 = (short) (Z + S);
          x3 = (short) (X + S);
          y3 = yb;
          z3 = (short) (Z + S);
        } else if (f == 3) {
          x0 = X;
          y0 = y01;
          z0 = Z;
          x1 = (short) (X + S);
          y1 = y11;
          z1 = Z;
          x2 = (short) (X + S);
          y2 = yb;
          z2 = Z;
          x3 = X;
          y3 = yb;
          z3 = Z;
        } else if (f == 4) {
          x0 = (short) (X + S);
          y0 = y11;
          z0 = Z;
          x1 = (short) (X + S);
          y1 = y10;
          z1 = (short) (Z + S);
          x2 = (short) (X + S);
          y2 = yb;
          z2 = (short) (Z + S);
          x3 = (short) (X + S);
          y3 = yb;
          z3 = Z;
        } else {
          x0 = X;
          y0 = y00;
          z0 = (short) (Z + S);
          x1 = X;
          y1 = y01;
          z1 = Z;
          x2 = X;
          y2 = yb;
          z2 = Z;
          x3 = X;
          y3 = yb;
          z3 = (short) (Z + S);
        }
        tmpVerts[o] = x0;
        tmpVerts[o + 1] = y0;
        tmpVerts[o + 2] = z0;
        tmpVerts[o + 3] = x1;
        tmpVerts[o + 4] = y1;
        tmpVerts[o + 5] = z1;
        tmpVerts[o + 6] = x2;
        tmpVerts[o + 7] = y2;
        tmpVerts[o + 8] = z2;
        tmpVerts[o + 9] = x3;
        tmpVerts[o + 10] = y3;
        tmpVerts[o + 11] = z3;
        tmpTexs[o / 3 * 2] = 0;
        tmpTexs[o / 3 * 2 + 1] = 0;
        tmpTexs[o / 3 * 2 + 2] = 1;
        tmpTexs[o / 3 * 2 + 3] = 0;
        tmpTexs[o / 3 * 2 + 4] = 1;
        tmpTexs[o / 3 * 2 + 5] = 1;
        tmpTexs[o / 3 * 2 + 6] = 0;
        tmpTexs[o / 3 * 2 + 7] = 1;
        for (int i = 0; i < 4; i++) {
          tmpCols[o + i * 3] = r;
          tmpCols[o + i * 3 + 1] = g;
          tmpCols[o + i * 3 + 2] = b;
        }
        int ii = (o / 12) * 6, vs = o / 3;
        tmpIndices[ii] = vs;
        tmpIndices[ii + 1] = vs + 1;
        tmpIndices[ii + 2] = vs + 2;
        tmpIndices[ii + 3] = vs;
        tmpIndices[ii + 4] = vs + 2;
        tmpIndices[ii + 5] = vs + 3;
      }
    }

    private int getCH(int x, int y, int z) {
      return (gF(x, y, z) + gF(x - 1, y, z) + gF(x, y, z - 1) + gF(x - 1, y, z - 1)) / 4;
    }

    private int gF(int x, int y, int z) {
      byte b = getBlock(x, y, z);
      if (b >= CARPET_BLACK && b <= CARPET_WHITE) return 2;
      if (isWater(b) || isLava(b)) {
        if (b == WATER || b == LAVA) return V_SCALE;
        int l = getData(x, y, z);
        if (l >= 8) l = 0;
        return Math.max(1, (8 - l) * V_SCALE / 9);
      } else if (b == SNOW_LAYER) {
        return 2;
      } else if (b != AIR && !it(b)) {
        return V_SCALE;
      }
      return 0;
    }

    class Drop {
      float x, y, z, vy, vx, vz, rot;
      byte type;
      int pickupTimer, count;

      public Drop(float x, float y, float z, byte t, float vx, float vy, float vz, int c, int d) {
        this.x = x;
        this.y = y;
        this.z = z;
        type = t;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        count = c;
        pickupTimer = d;
      }
    }

    public MCanvas(MIDlet m) {
      super(false);
      try {
        imgArmorHelmet = Image.createImage("/j2me_textures/gui/empty_armor_slot_helmet.png");
        imgArmorChest = Image.createImage("/j2me_textures/gui/empty_armor_slot_chestplate.png");
        imgArmorLegs = Image.createImage("/j2me_textures/gui/empty_armor_slot_leggings.png");
        imgArmorBoots = Image.createImage("/j2me_textures/gui/empty_armor_slot_boots.png");
        try {
          imgBubblePop = Image.createImage("/j2me_textures/gui/bubble_pop.png");
        } catch (Exception e) {
        }
        try {
          imgBubble = Image.createImage("/j2me_textures/gui/bubble.png");
        } catch (Exception e) {
        }
        try {
          imgArmorHalf = Image.createImage("/j2me_textures/gui/armor_half.png");
        } catch (Exception e) {
        }
        try {
          imgArmorFull = Image.createImage("/j2me_textures/gui/armor_full.png");
        } catch (Exception e) {
        }
        try {
          imgHpEmpty = Image.createImage("/j2me_textures/gui/hp_empty.png");
        } catch (Exception e) {
        }
        try {
          imgHpHalf = Image.createImage("/j2me_textures/gui/hp_half.png");
        } catch (Exception e) {
        }
        try {
          imgHpFull = Image.createImage("/j2me_textures/gui/hp_full.png");
        } catch (Exception e) {
        }
        try {
          imgHungerEmpty = Image.createImage("/j2me_textures/gui/hunger_empty.png");
        } catch (Exception e) {
        }
        try {
          imgHungerHalf = Image.createImage("/j2me_textures/gui/hunger_half.png");
        } catch (Exception e) {
        }
        try {
          imgHungerFull = Image.createImage("/j2me_textures/gui/hunger_full.png");
        } catch (Exception e) {
        }
      } catch (Exception e) {
        System.out.println("Error loading GUI textures");
      }
      try {
        logoImg = Image.createImage("/j2me_textures/menu/minecraft_logo.png");
      } catch (Exception e) {
      }
      try {
        profileImg = Image.createImage("/j2me_textures/menu/profile.png");
      } catch (Exception e) {
      }
      loadSettings();
      setFullScreenMode(true);
      debugFont = Font.getFont(0, 0, 8);
      btnFont = Font.getFont(64, 1, 16);
      invFont = Font.getFont(0, 0, 8);
      for (int i = 0; i < 9; i++) hotbar[i] = new Slot();
      for (int i = 0; i < 27; i++) inventory[i] = new Slot();
      for (int i = 0; i < 4; i++) armor[i] = new Slot();
      for (int i = 0; i < 9; i++) craft[i] = new Slot();
      chunks = new Chunk[CHUNKS_X * CHUNKS_Z];
      for (int x = 0; x < CHUNKS_X; x++)
        for (int z = 0; z < CHUNKS_Z; z++) chunks[x + z * CHUNKS_X] = new Chunk(x, z);
    }

    public void stop() {
      running = false;
    }

    public void setPaused(boolean p) {
      if (p
          && (gameState == 1
              || gameState == 5
              || gameState == 4
              || gameState == 6
              || gameState == 8)) {
        gameState = 2;
        menuSelection = 0;
      }
    }

    protected void keyPressed(int k) {
      if (k == -7) {
        if (world != null) {
          javax.microedition.lcdui.Display.getDisplay(MinecraftMIDlet.this)
              .setCurrent(MinecraftMIDlet.this._chatBox);
        }
        return;
      }
      int act = 0;
      try {
        act = getGameAction(k);
      } catch (Exception e) {
      }
      if (k == -1 || k == 50 || act == UP) {
        long now = System.currentTimeMillis();
        if (now - lastForwardTime < 300) {
          isSprinting = true;
        }
        lastForwardTime = now;
      }
      if (k == 48) {
        long t = System.currentTimeMillis();
        if (creativeMode && t - lastJumpTime < 400) {
          isFlying = !isFlying;
          vy = 0;
        }
        lastJumpTime = t;
      }
      updateKeys(k, true);
    }

    protected void keyReleased(int k) {
      updateKeys(k, false);
    }

    private void updateKeys(int k, boolean p) {
      boolean num = false;
      if (k == 35) {
        k_pound = p;
        num = true;
      }
      if (k == 42) {
        k_star = p;
        num = true;
      }
      if (k == 48) {
        k_0 = p;
        num = true;
      }
      if (k == 49) {
        k_1 = p;
        num = true;
      }
      if (k == 50) {
        k_2 = p;
        num = true;
      }
      if (k == 51) {
        k_3 = p;
        num = true;
      }
      if (k == 52) {
        k_4 = p;
        num = true;
      }
      if (k == 53 || k == 70 || k == 102 || k == -5) {
        k_5 = p;
        k_fire = p;
        num = true;
      }
      if (k == 54) {
        k_6 = p;
        num = true;
      }
      if (k == 55) {
        k_7 = p;
        num = true;
      }
      if (k == 76 || k == 108 || k == -6 || k == -21 || k == -20) {
        k_lsk = p;
        num = true;
      }
      if (k == 56) {
        k_8 = p;
        num = true;
      }
      if (k == 57) {
        k_9 = p;
        num = true;
      }
      if (!num) {
        int act = 0;
        try {
          act = getGameAction(k);
        } catch (Exception e) {
        }
        if (k == -1 || act == UP) k_up = p;
        if (k == -2 || act == DOWN) k_down = p;
        if (k == -3 || act == LEFT) k_left = p;
        if (k == -4 || act == RIGHT) k_right = p;
        if (k == -5 || act == FIRE) k_fire = p;
      }
    }

    private void loadPanorama() {
      try {
        panoCam = new Camera();
        panoCam.setPerspective(90.0f, (float) getWidth() / getHeight(), 0.1f, 100.0f);
        short[] v = {
          -10, -10, -10, 10, -10, -10, 10, 10, -10, -10, 10, -10, 10, -10, -10, 10, -10, 10, 10, 10,
          10, 10, 10, -10, 10, -10, 10, -10, -10, 10, -10, 10, 10, 10, 10, 10, -10, -10, 10, -10,
          -10, -10, -10, 10, -10, -10, 10, 10, -10, 10, -10, 10, 10, -10, 10, 10, 10, -10, 10, 10,
          -10, -10, 10, 10, -10, 10, 10, -10, -10, -10, -10, -10
        };
        VertexArray va = new VertexArray(24, 3, 2);
        va.set(0, 24, v);
        short[] t = new short[48];
        for (int i = 0; i < 6; i++) {
          int o = i * 8;
          t[o] = 0;
          t[o + 1] = 1;
          t[o + 2] = 1;
          t[o + 3] = 1;
          t[o + 4] = 1;
          t[o + 5] = 0;
          t[o + 6] = 0;
          t[o + 7] = 0;
        }
        VertexArray ta = new VertexArray(24, 2, 2);
        ta.set(0, 24, t);
        VertexBuffer vb = new VertexBuffer();
        vb.setPositions(va, 1.0f, null);
        vb.setTexCoords(0, ta, 1.0f, null);
        vb.setDefaultColor(0xFFFFFFFF);
        IndexBuffer[] ibs = new IndexBuffer[6];
        Appearance[] apps = new Appearance[6];
        int[] ind = {0, 1, 2, 0, 2, 3};
        int[] fCols = {0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFF00, 0xFF00FFFF, 0xFFFF00FF};
        for (int i = 0; i < 6; i++) {
          int[] fi = new int[6];
          for (int k = 0; k < 6; k++) fi[k] = ind[k] + (i * 4);
          ibs[i] = new TriangleStripArray(fi, new int[] {3, 3});
          apps[i] = new Appearance();
          PolygonMode pm = new PolygonMode();
          pm.setCulling(PolygonMode.CULL_NONE);
          pm.setShading(PolygonMode.SHADE_FLAT);
          apps[i].setPolygonMode(pm);
          Material mat = new Material();
          mat.setColor(Material.EMISSIVE, fCols[i % 6]);
          mat.setColor(Material.DIFFUSE, 0x00000000);
          mat.setColor(Material.AMBIENT, 0x00000000);
          mat.setVertexColorTrackingEnable(false);
          apps[i].setMaterial(mat);
          try {
            Image img = Image.createImage("/j2me_textures/menu/panorama_" + i + ".png");
            Texture2D tex = new Texture2D(new Image2D(Image2D.RGB, img));
            tex.setWrapping(Texture2D.WRAP_CLAMP, Texture2D.WRAP_CLAMP);
            tex.setFiltering(Texture2D.FILTER_LINEAR, Texture2D.FILTER_LINEAR);
            tex.setBlending(Texture2D.FUNC_REPLACE);
            apps[i].setTexture(0, tex);
          } catch (Exception e) {
          }
        }
        panoMesh = new Mesh(vb, ibs, apps);
        panoInit = true;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    private void renderPanorama(Graphics g) {
      if (!panoInit) loadPanorama();
      if (panoMesh == null) {
        renderPanorama(g);
        return;
      }
      g3d.bindTarget(g);
      g3d.clear(null);
      Transform t = new Transform();
      t.postRotate(panoRot, 0, 1, 0);
      panoRot += 0.1f;
      g3d.setCamera(panoCam, t);
      g3d.render(panoMesh, null);
      g3d.releaseTarget();
    }

    public void run() {
      try {
        initM3G();
      } catch (Exception e) {
        e.printStackTrace();
      }
      lastTime = System.currentTimeMillis();
      while (running) {
        if (MinecraftMIDlet.this.musicSys != null) {
          if (setMusic > 0) {
            boolean ig =
                (gameState == 1
                    || gameState == 2
                    || gameState == 4
                    || gameState == 5
                    || gameState == 6
                    || gameState == 8);
            MinecraftMIDlet.this.musicSys.setContext(ig);
            MinecraftMIDlet.this.musicSys.start();
          } else {
            MinecraftMIDlet.this.musicSys.stop();
          }
        }
        long s = System.currentTimeMillis(), dt = s - lastTime;
        lastTime = s;
        frameCount++;
        if (frameCount % 30 == 0) fps = (int) (1000 / (dt == 0 ? 1 : dt));
        if (gameState == 0) {
          long now = System.currentTimeMillis();
          if (now - lastNavTime > 150) {
            boolean moved = false;
            if (k_up || k_2) {
              menuSelection--;
              if (menuSelection < 0) menuSelection = 2;
              moved = true;
            } else if (k_down || k_8) {
              menuSelection++;
              if (menuSelection > 2) menuSelection = 0;
              moved = true;
            }
            if (k_right || k_6) {
              if (menuSelection == 1) {
                menuSelection = 2;
                moved = true;
              }
            }
            if (k_left || k_4) {
              if (menuSelection == 2) {
                menuSelection = 1;
                moved = true;
              }
            }
            if (moved) {
              lastNavTime = now;
              k_up = false;
              k_2 = false;
              k_down = false;
              k_8 = false;
              k_left = false;
              k_4 = false;
              k_right = false;
              k_6 = false;
            }
          }
          if (k_fire && (System.currentTimeMillis() - lastActionTime > 300)) {
            lastActionTime = System.currentTimeMillis();
            if (menuSelection == 0) {
              gameState = 7;
              menuSelection = 0;
            } else if (menuSelection == 1) {
              gameState = 3;
              menuSelection = 0;
              settingsPage = 0;
            } else if (menuSelection == 2) {
              gameState = 9;
              menuSelection = 0;
            }
            k_fire = false;
          }
          renderMenu();
        } else if (gameState == 9) {
          hm(2);
          if (k_fire && (System.currentTimeMillis() - lastActionTime > 300)) {
            lastActionTime = System.currentTimeMillis();
            if (menuSelection == 0) {
              MinecraftMIDlet.this.showNickInput();
            } else {
              gameState = 0;
              menuSelection = 2;
            }
            k_fire = false;
          }
          renderProfile();
        } else if (gameState == 7) {
          hm(5);
          if (k_fire && (System.currentTimeMillis() - lastActionTime > 300)) {
            lastActionTime = System.currentTimeMillis();
            if (menuSelection == 0) setupMode = (setupMode + 1) % 2;
            else if (menuSelection == 1) setupType = (setupType + 1) % 2;
            else if (menuSelection == 2) {
              MinecraftMIDlet.this.showSeedInput();
            } else if (menuSelection == 3) {
              creativeMode = (setupMode == 1);
              isFlying = false;
              generateWorld();
              gameState = 1;
            } else {
              gameState = 0;
              menuSelection = 0;
            }
            k_fire = false;
          }
          renderSetup();
        } else if (gameState == 2) {
          hm(3);
          if (k_fire && (System.currentTimeMillis() - lastActionTime > 300)) {
            lastActionTime = System.currentTimeMillis();
            if (menuSelection == 0) gameState = 1;
            else if (menuSelection == 1) {
              gameState = 3;
              menuSelection = 0;
              settingsPage = 0;
            } else if (menuSelection == 2) {
              gameState = 0;
              world = null;
              worldData = null;
              drops.removeAllElements();
              fallingBlocks.removeAllElements();
              System.gc();
            }
            k_fire = false;
          }
          renderPause();
        } else if (gameState == 3) {
          int maxItems = 4;
          if (settingsPage == 1) maxItems = 8;
          else if (settingsPage == 2) maxItems = 2;
          else if (settingsPage == 3) maxItems = 3;
          long now = System.currentTimeMillis();
          if (now - lastNavTime > 150) {
            boolean moved = false;
            if (settingsPage == 1) {
              if (k_down || k_8) {
                if (menuSelection == 6 || menuSelection == 7) menuSelection = 0;
                else if (menuSelection == 4) menuSelection = 6;
                else if (menuSelection == 5) menuSelection = 7;
                else menuSelection += 2;
                moved = true;
              } else if (k_up || k_2) {
                if (menuSelection == 0 || menuSelection == 1) menuSelection = 6;
                else if (menuSelection == 6) menuSelection = 4;
                else if (menuSelection == 7) menuSelection = 5;
                else menuSelection -= 2;
                moved = true;
              } else if (k_left || k_4) {
                if (menuSelection % 2 != 0) menuSelection--;
                moved = true;
              } else if (k_right || k_6) {
                if (menuSelection % 2 == 0) menuSelection++;
                moved = true;
              }
            } else {
              if (k_down || k_8) {
                menuSelection++;
                if (menuSelection >= maxItems) menuSelection = 0;
                moved = true;
              }
              if (k_up || k_2) {
                menuSelection--;
                if (menuSelection < 0) menuSelection = maxItems - 1;
                moved = true;
              }
              if (settingsPage == 2 && menuSelection == 0) {
                if (k_left || k_4) {
                  setMusic -= 5;
                  if (setMusic < 0) setMusic = 0;
                  saveSettings();
                  k_left = false;
                  k_4 = false;
                  lastNavTime = now;
                }
                if (k_right || k_6) {
                  setMusic += 5;
                  if (setMusic > 100) setMusic = 100;
                  saveSettings();
                  k_right = false;
                  k_6 = false;
                  lastNavTime = now;
                }
              }
            }
            if (moved) {
              lastNavTime = now;
              k_up = false;
              k_2 = false;
              k_down = false;
              k_8 = false;
              if (!(settingsPage == 2 && menuSelection == 0)) {
                k_left = false;
                k_4 = false;
                k_right = false;
                k_6 = false;
              }
            }
          }
          if ((k_fire || k_5) && (System.currentTimeMillis() - lastActionTime > 300)) {
            lastActionTime = System.currentTimeMillis();
            if (settingsPage == 0) {
              if (menuSelection == 0) {
                settingsPage = 1;
                menuSelection = 0;
              } else if (menuSelection == 1) {
                settingsPage = 2;
                menuSelection = 0;
              } else if (menuSelection == 2) {
                settingsPage = 3;
                menuSelection = 0;
              } else if (menuSelection == 3) {
                if (world == null) gameState = 0;
                else gameState = 2;
                menuSelection = 0;
              }
            } else if (settingsPage == 1) {
              if (menuSelection == 0) {
                setDrops = (setDrops + 1) % 2;
                saveSettings();
              } else if (menuSelection == 1) {
                setLiquid = (setLiquid + 1) % 2;
                saveSettings();
              } else if (menuSelection == 2) {
                setClouds = (setClouds + 1) % 3;
                saveSettings();
              } else if (menuSelection == 3) {
                setEffects = (setEffects + 1) % 2;
                saveSettings();
              } else if (menuSelection == 4) {
                setAnimations = (setAnimations + 1) % 2;
                saveSettings();
                if (chunks != null) for (int i = 0; i < chunks.length; i++) chunks[i].dirty = true;
              } else if (menuSelection == 5) {
                setLight = (setLight + 1) % 3;
                saveSettings();
                if (chunks != null) for (int i = 0; i < chunks.length; i++) chunks[i].dirty = true;
              } else if (menuSelection == 6) {
                setChunks = (setChunks % 4) + 1;
                saveSettings();
                if (chunks != null) for (int i = 0; i < chunks.length; i++) chunks[i].dirty = true;
              } else if (menuSelection == 7) {
                settingsPage = 0;
                menuSelection = 0;
              }
            } else if (settingsPage == 2) {
              if (menuSelection == 0) {
                if (setMusic > 0) setMusic = 0;
                else setMusic = 100;
                saveSettings();
              } else if (menuSelection == 1) {
                settingsPage = 0;
                menuSelection = 1;
              }
            } else if (settingsPage == 3) {
              if (menuSelection == 0) {
                showFPS = !showFPS;
                saveSettings();
              } else if (menuSelection == 1) {
                showXYZ = !showXYZ;
                saveSettings();
              } else if (menuSelection == 2) {
                settingsPage = 0;
                menuSelection = 2;
              }
            }
            k_fire = false;
            k_5 = false;
          }
          renderSettings();
        } else if (gameState == 4 || gameState == 5 || gameState == 6 || gameState == 8) {
          updateInventory((int) dt);
          if (gameState == 6) updateFurnace((int) dt);
          renderInventory();
        } else {
          if (k_star) {
            setPaused(true);
            k_star = false;
          } else if (k_pound) {
            openInventory(0);
            k_pound = false;
          } else {
            updatePortalAnim();
            updateGame((int) dt);
            updateFurnace((int) dt);
            updateFluids();
            updateFire();
          }
          renderGame();
        }
        flushGraphics();
        long e = System.currentTimeMillis();
        if (e - s < 20)
          try {
            Thread.sleep(20 - (e - s));
          } catch (Exception x) {
          }
      }
    }

    private void hm(int m) {
      long now = System.currentTimeMillis();
      if (now - lastNavTime < 150) return;
      boolean moved = false;
      if (k_up || k_2) {
        menuSelection--;
        if (menuSelection < 0) menuSelection = m - 1;
        moved = true;
      } else if (k_down || k_8) {
        menuSelection++;
        if (menuSelection >= m) menuSelection = 0;
        moved = true;
      }
      if (moved) lastNavTime = now;
    }

    private boolean isWater(byte id) {
      return id == WATER || id == WATER_FLOW;
    }

    private boolean isLava(byte id) {
      return id == LAVA || id == LAVA_FLOW;
    }

    private boolean isFlammable(byte id) {
      return id == LEAVES || id == WOOD || id == PLANKS || id == WORKBENCH;
    }

    private boolean it(byte id) {
      if (id == GRASS_PATH
          || id == LADDER
          || id == SLAB_COBBLE
          || id == SLAB_OAK
          || id == GLASS_PANE) return true;
      if (id >= PLATE_OAK && id <= PLATE_STONE) return true;
      if (id >= CARPET_BLACK && id <= CARPET_WHITE) return true;
      return id == WHEAT_BLOCK
          || isCrossed(id)
          || isWater(id)
          || isLava(id)
          || id == WATER_FLOW
          || id == LAVA_FLOW
          || id == GLASS
          || id == LEAVES
          || id == FIRE
          || id == PORTAL
          || id == REDSTONE
          || id == WOOD_DOOR
          || id == NETHER_FENCE
          || id == NETHER_STAIRS
          || id == NETHER_WART
          || id == ICE
          || id == SHORT_GRASS
          || id == PLANT_TALL_GRASS
          || id == FLOWER_YELLOW
          || id == FLOWER_RED
          || id == REEDS
          || id == CACTUS
          || id == WEB
          || id == DEAD_BUSH
          || id == SNOW_LAYER
          || id == LEAVES_BIRCH
          || id == LEAVES_SPRUCE
          || id == LEAVES_JUNGLE
          || id == LEAVES_ACACIA
          || id == LEAVES_DARK_OAK
          || id == FENCE
          || id == STAIRS_WOOD
          || id == STAIRS_COBBLE
          || id == IRON_BARS
          || id == GLASS_PANE;
    }

    private void updateFluids() {
      if (setLiquid == 1) return;
      fluidTickTimer++;
      if (fluidTickTimer < 5) return;
      fluidTickTimer = 0;
      int cx = (int) px, cy = (int) py, cz = (int) pz, rad = 16;
      for (int x = cx - rad; x <= cx + rad; x++) {
        for (int z = cz - rad; z <= cz + rad; z++) {
          for (int y = 0; y < 64; y++) {
            byte id = getBlock(x, y, z);
            if (isWater(id)) sf(x, y, z, id, WATER, WATER_FLOW, 7);
            else if (isLava(id)) sf(x, y, z, id, LAVA, LAVA_FLOW, 4);
          }
        }
      }
    }

    private void updateFire() {
      fireTickTimer++;
      if (fireTickTimer < 12) return;
      fireTickTimer = 0;
      int rad = 10;
      int cx = (int) px, cy = (int) py, cz = (int) pz;
      for (int dx = -rad; dx <= rad; dx++) {
        for (int dz = -rad; dz <= rad; dz++) {
          for (int dy = -rad; dy <= rad; dy++) {
            int x = cx + dx, y = cy + dy, z = cz + dz;
            if (x < 0 || x >= WORLD_X || y < 0 || y >= WORLD_H || z < 0 || z >= WORLD_Y) continue;
            byte b = getBlock(x, y, z);
            if (b == FARMLAND && rand.nextInt(20) == 0) {
              if (isHydrated(x, y, z)) {
                setData(x, y, z, 7);
                markChunkDirtyAt(x, z);
              } else {
                int d = getData(x, y, z);
                if (d > 0) {
                  setData(x, y, z, d - 1);
                  markChunkDirtyAt(x, z);
                }
              }
            }
            if (b == WHEAT_BLOCK && rand.nextInt(20) == 0) {
              int age = getData(x, y, z);
              if (age < 7) {
                int m = getData(x, y - 1, z);
                if (m > 0 || rand.nextInt(3) == 0) {
                  setData(x, y, z, age + 1);
                  markChunkDirtyAt(x, z);
                }
              }
            }
            if (b == FIRE) {
              byte above = getBlock(x, y + 1, z);
              if (above != AIR && !it(above)) {
                setBlockAndDirty(x, y, z, AIR);
                continue;
              }
              boolean extinguished = false;
              outer:
              for (int ddx = -1; ddx <= 1; ddx++)
                for (int ddy = -1; ddy <= 1; ddy++)
                  for (int ddz = -1; ddz <= 1; ddz++)
                    if (!(ddx == 0 && ddy == 0 && ddz == 0)
                        && isWater(getBlock(x + ddx, y + ddy, z + ddz))) {
                      extinguished = true;
                      break outer;
                    }
              if (extinguished) {
                setBlockAndDirty(x, y, z, AIR);
                continue;
              }
              boolean supported = false;
              outer2:
              for (int ddx = -1; ddx <= 1; ddx++)
                for (int ddy = -1; ddy <= 1; ddy++)
                  for (int ddz = -1; ddz <= 1; ddz++)
                    if (Math.abs(ddx) + Math.abs(ddy) + Math.abs(ddz) == 1
                        && isFlammable(getBlock(x + ddx, y + ddy, z + ddz))) {
                      supported = true;
                      break outer2;
                    }
              if (!supported && rand.nextInt(4) == 0) {
                setBlockAndDirty(x, y, z, AIR);
                continue;
              }
              int[][] dirs = {{0, -1, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, 1, 0}};
              for (int i = 0; i < dirs.length; i++) {
                int[] d = dirs[i];
                int nx = x + d[0], ny = y + d[1], nz = z + d[2];
                if (nx < 0 || nx >= WORLD_X || ny < 0 || ny >= WORLD_H || nz < 0 || nz >= WORLD_Y)
                  continue;
                byte nb = getBlock(nx, ny, nz);
                if (nb == TNT) {
                  primeTNT(nx, ny, nz);
                } else if (isFlammable(nb)) {
                  int chance = (d[1] == 1) ? 10 : 3;
                  if (rand.nextInt(chance) == 0) {
                    setBlockAndDirty(nx, ny, nz, FIRE);
                  }
                } else if (nb == AIR && d[1] == -1) {
                  if (rand.nextInt(3) == 0) {
                    setBlockAndDirty(nx, ny, nz, FIRE);
                  }
                }
              }
            }
          }
        }
      }
    }

    private void sf(int x, int y, int z, byte id, byte si, byte fi, int md) {
      int dist = getData(x, y, z);
      int cDist = md + 1;
      byte up = getBlock(x, y + 1, z);
      if (up == si || up == fi) {
        cDist = 0;
      } else if (id == si) {
        cDist = 0;
      } else {
        int min = 100;
        int[][] d = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int i = 0; i < 4; i++) {
          byte nb = getBlock(x + d[i][0], y, z + d[i][1]);
          if (nb == si) {
            min = -1;
            break;
          }
          if (nb == fi) {
            int nd = getData(x + d[i][0], y, z + d[i][1]);
            if (nd < min) min = nd;
          }
        }
        cDist = min + 1;
      }
      if (cDist != dist) {
        if (cDist > md) setBlockAndDirty(x, y, z, AIR);
        else {
          setData(x, y, z, cDist);
          markChunkDirtyAt(x, z);
        }
        return;
      }
      if (dist >= md) return;
      byte dw = getBlock(x, y - 1, z);
      if (cf(dw)) {
        if (dw == fi && getData(x, y - 1, z) == 0) return;
        setBlock(x, y - 1, z, fi);
        setData(x, y - 1, z, 0);
        inf(x, y - 1, z, fi);
        markChunkDirtyAt(x, z);
        return;
      }
      int[][] d = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
      boolean[] h = new boolean[4];
      boolean hasH = false;
      for (int i = 0; i < 4; i++) {
        int dx = x + d[i][0], dz = z + d[i][1];
        byte n = getBlock(dx, y, dz);
        if (cf(n) && cf(getBlock(dx, y - 1, dz))) {
          h[i] = true;
          hasH = true;
        }
      }
      int nd = (id == si) ? 1 : dist + 1;
      for (int i = 0; i < 4; i++) {
        if (hasH && !h[i]) continue;
        int dx = x + d[i][0], dz = z + d[i][1];
        byte n = getBlock(dx, y, dz);
        if (cf(n)) {
          if (n == AIR
              || (isWater(n) && getData(dx, y, dz) > nd)
              || (isLava(n) && getData(dx, y, dz) > nd)) {
            setBlock(dx, y, dz, fi);
            setData(dx, y, dz, nd);
            inf(dx, y, dz, fi);
            markChunkDirtyAt(dx, dz);
          }
        }
      }
    }

    private boolean cf(byte id) {
      return id == AIR || isWater(id) || isLava(id) || id == WATER_FLOW || id == LAVA_FLOW;
    }

    private void inf(int x, int y, int z, byte s) {
      int[][] d = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {0, 0}};
      for (int i = 0; i < 5; i++) {
        byte n = getBlock(x + d[i][0], y, z + d[i][1]);
        if (isWater(s) && isLava(n)) ml(x + d[i][0], y, z + d[i][1], n);
        else if (isLava(s) && isWater(n)) ml(x, y, z, s);
      }
      byte t = getBlock(x, y + 1, z);
      if (isLava(s) && isWater(t)) setBlock(x, y, z, OBSIDIAN);
    }

    private void ml(int x, int y, int z, byte l) {
      if (l == LAVA) setBlock(x, y, z, OBSIDIAN);
      else setBlock(x, y, z, COBBLE);
    }

    private void openInventory(int t) {
      if (t == 1) gameState = 5;
      else if (t == 2) {
        gameState = 6;
        FurnaceTE f = getFurnaceAt(targetX, targetY, targetZ);
        if (f == null) {
          createTileEntity(targetX, targetY, targetZ, FURNACE);
          f = getFurnaceAt(targetX, targetY, targetZ);
        }
        furnaceIn = f.in;
        furnaceFuel = f.fuel;
        furnaceOut = f.out;
        burnTime = f.bTime;
        cookTime = f.cTime;
        burnTimeMax = f.bMax;
      } else if (t == 3) {
        gameState = 8;
        invSection = SEC_CHEST;
        ChestTE te = getChestAt(targetX, targetY, targetZ);
        if (te == null) {
          createTileEntity(targetX, targetY, targetZ, CHEST);
          te = getChestAt(targetX, targetY, targetZ);
        }
        validateChestLink(te);
        ChestTE pair = te.pair;
        int size = (pair != null) ? 54 : 27;
        chestInv = new Slot[size];
        if (pair == null) {
          System.arraycopy(te.items, 0, chestInv, 0, 27);
        } else {
          ChestTE main = te;
          ChestTE sec = pair;
          if (pair.x < te.x || pair.z < te.z) {
            main = pair;
            sec = te;
          }
          System.arraycopy(main.items, 0, chestInv, 0, 27);
          System.arraycopy(sec.items, 0, chestInv, 27, 27);
        }
        chestRows = (size + 8) / 9;
      } else gameState = 4;
      int w = getWidth();
      int h = getHeight();
      int pad = 2;
      if (w < h) {
        int cols = 10;
        int colWidth = (w - 4) / cols;
        slSz = colWidth - pad;
        if (slSz < 10) slSz = 10;
        int totalW = cols * (slSz + pad);
        guiOx = (w - totalW) / 2;
      } else {
        slSz = 20;
        int gridW = 9 * (slSz + pad);
        guiOx = (w - gridW) / 2;
      }
      invSection = SEC_HOTBAR;
      invCursorX = selectedSlot;
      invCursorY = 0;
      int th = (slSz + 2) * 9;
      guiOy = (h - th) / 2;
      if (guiOy < 0) guiOy = 0;
      for (int i = 0; i < 9; i++) {
        if (craft[i].count > 0) addToInventory(craft[i].id, craft[i].count);
        craft[i].id = 0;
        craft[i].count = 0;
      }
      if (gameState != 6) updateCrafting();
      creativeTab = 1;
      libScroll = 0;
    }

    private void closeInventory() {
      gameState = 1;
      chestInv = null;
      if (cursor.count > 0) {
        drops.addElement(new Drop(px, py + 1.5f, pz, cursor.id, 0, 0.1f, 0, cursor.count, 2000));
        cursor.id = 0;
        cursor.count = 0;
      }
      for (int i = 0; i < 9; i++) {
        if (craft[i].count > 0) {
          addToInventory(craft[i].id, craft[i].count);
          craft[i].id = 0;
          craft[i].count = 0;
        }
      }
      craftResult.id = 0;
      craftResult.count = 0;
    }

    abstract class TileEntity {
      int x, y, z;
    }

    class ChestTE extends TileEntity {
      Slot[] items;
      ChestTE pair;

      ChestTE(int sz, int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        items = new Slot[sz];
        for (int i = 0; i < sz; i++) items[i] = new Slot();
      }
    }

    class FurnaceTE extends TileEntity {
      Slot in = new Slot(), fuel = new Slot(), out = new Slot();
      int bTime = 0, bMax = 0, cTime = 0;

      FurnaceTE(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
      }
    }

    private String getTeKey(int x, int y, int z) {
      return x + "," + y + "," + z;
    }

    private void createTileEntity(int x, int y, int z, int id) {
      if (id == CHEST) tileEntities.put(getTeKey(x, y, z), new ChestTE(27, x, y, z));
      if (id == FURNACE) tileEntities.put(getTeKey(x, y, z), new FurnaceTE(x, y, z));
    }

    private void removeTileEntity(int x, int y, int z) {
      String k = getTeKey(x, y, z);
      TileEntity te = (TileEntity) tileEntities.get(k);
      if (te != null) {
        if (te instanceof ChestTE) {
          ChestTE c = (ChestTE) te;
          if (c.pair != null) {
            c.pair.pair = null;
            c.pair = null;
          }
          for (int i = 0; i < c.items.length; i++)
            if (c.items[i].count > 0)
              drops.addElement(
                  new Drop(
                      x + 0.5f,
                      y + 0.5f,
                      z + 0.5f,
                      c.items[i].id,
                      0,
                      0.1f,
                      0,
                      c.items[i].count,
                      500));
        } else if (te instanceof FurnaceTE) {
          FurnaceTE f = (FurnaceTE) te;
          if (f.in.count > 0)
            drops.addElement(
                new Drop(x + 0.5f, y + 0.5f, z + 0.5f, f.in.id, 0, 0.1f, 0, f.in.count, 500));
          if (f.fuel.count > 0)
            drops.addElement(
                new Drop(x + 0.5f, y + 0.5f, z + 0.5f, f.fuel.id, 0, 0.1f, 0, f.fuel.count, 500));
          if (f.out.count > 0)
            drops.addElement(
                new Drop(x + 0.5f, y + 0.5f, z + 0.5f, f.out.id, 0, 0.1f, 0, f.out.count, 500));
        }
        tileEntities.remove(k);
      }
    }

    private ChestTE getChestAt(int x, int y, int z) {
      return (ChestTE) tileEntities.get(getTeKey(x, y, z));
    }

    private FurnaceTE getFurnaceAt(int x, int y, int z) {
      return (FurnaceTE) tileEntities.get(getTeKey(x, y, z));
    }

    private boolean isChest(int x, int y, int z) {
      return getBlock(x, y, z) == CHEST;
    }

    private void validateChestLink(ChestTE me) {
      if (me.pair != null) {
        if (!isChest(me.pair.x, me.pair.y, me.pair.z)) me.pair = null;
        else if (me.pair.pair != me) me.pair = null;
        else return;
      }
      int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
      int neighborCount = 0;
      ChestTE candidate = null;
      for (int i = 0; i < 4; i++) {
        int nx = me.x + dirs[i][0], nz = me.z + dirs[i][1];
        if (isChest(nx, me.y, nz)) {
          neighborCount++;
          ChestTE nb = getChestAt(nx, me.y, nz);
          if (nb == null) {
            createTileEntity(nx, me.y, nz, CHEST);
            nb = getChestAt(nx, me.y, nz);
          }
          candidate = nb;
        }
      }
      if (neighborCount != 1) {
        if (me.pair != null) {
          me.pair.pair = null;
          me.pair = null;
        }
        return;
      }
      if (candidate != null) {
        if (candidate.pair != null && candidate.pair != me) return;
        int nbNeighbors = 0;
        for (int i = 0; i < 4; i++) {
          if (isChest(candidate.x + dirs[i][0], candidate.y, candidate.z + dirs[i][1]))
            nbNeighbors++;
        }
        if (nbNeighbors == 1) {
          me.pair = candidate;
          candidate.pair = me;
        }
      }
    }

    private void updateInventory(int dt) {
      if (tooltipTimer > 0) tooltipTimer -= dt;
      boolean nt = (gameState == 5 || gameState == 6 || gameState == 8);
      if (creativeMode && invSection == SEC_TABS && !nt) {
        if (k_left || k_4) {
          creativeTab = 0;
          k_left = false;
          k_4 = false;
        }
        if (k_right || k_6) {
          creativeTab = 1;
          k_right = false;
          k_6 = false;
        }
        if (k_down || k_8) {
          if (creativeTab == 0) {
            invSection = SEC_LIB;
            invCursorX = 0;
            invCursorY = 0;
          } else {
            invSection = SEC_CRAFT;
            invCursorX = 1;
            invCursorY = 0;
          }
          k_down = false;
          k_8 = false;
        }
      } else if (creativeMode && invSection == SEC_LIB) {
        int vR = 8;
        int tR = (libItems.length + 8) / 9;
        int mS = tR - vR;
        if (mS < 0) mS = 0;
        if (k_0) {
          if (k_up || k_2) {
            libScroll -= vR;
            if (libScroll < 0) libScroll = 0;
            k_up = false;
            k_2 = false;
          }
          if (k_down || k_8) {
            libScroll += vR;
            if (libScroll > mS) libScroll = mS;
            k_down = false;
            k_8 = false;
          }
        } else {
          if (k_up || k_2) {
            invCursorY--;
            if (invCursorY < 0) {
              if (!nt) {
                invSection = SEC_TABS;
                invCursorY = 0;
              } else invCursorY = 0;
            }
            k_up = false;
            k_2 = false;
          }
          if (k_down || k_8) {
            invCursorY++;
            if (invCursorY >= vR) {
              invSection = SEC_HOTBAR;
              invCursorY = 0;
            }
            k_down = false;
            k_8 = false;
          }
          if (k_left || k_4) {
            invCursorX--;
            if (invCursorX < 0) invCursorX = 8;
            k_left = false;
            k_4 = false;
          }
          if (k_right || k_6) {
            invCursorX++;
            if (invCursorX > 8) invCursorX = 0;
            k_right = false;
            k_6 = false;
          }
          int idx = invCursorX + (invCursorY + libScroll) * 9;
          if (idx < libItems.length) {
            byte i = libItems[idx];
            if (k_3) {
              cursor.id = i;
              cursor.count = 1;
              k_3 = false;
            }
            if (k_5 || k_1 || k_fire) {
              cursor.id = i;
              cursor.count = 64;
              k_5 = false;
              k_1 = false;
              k_fire = false;
            }
          }
        }
      } else {
        if (k_up || k_2) {
          moveCursor(0, -1);
          k_up = false;
          k_2 = false;
        }
        if (k_down || k_8) {
          moveCursor(0, 1);
          k_down = false;
          k_8 = false;
        }
        if (k_left || k_4) {
          moveCursor(-1, 0);
          k_left = false;
          k_4 = false;
        }
        if (k_right || k_6) {
          moveCursor(1, 0);
          k_right = false;
          k_6 = false;
        }
        if (invSection == SEC_DELETE) {
          if (k_3) {
            if (cursor.count > 0) {
              cursor.count--;
              if (cursor.count == 0) cursor.id = 0;
            }
            k_3 = false;
          }
          if (k_fire || k_5 || k_1) {
            cursor.id = 0;
            cursor.count = 0;
            k_fire = false;
            k_5 = false;
            k_1 = false;
          }
        }
        if (k_fire && (System.currentTimeMillis() - lastActionTime > 300)) {
          lastActionTime = System.currentTimeMillis();
          long now = System.currentTimeMillis();
          if (invSection != SEC_RESULT
              && invSection != SEC_FURNACE_OUT
              && now - lastClickTime < 300) {
            collectStack();
          } else {
            clickSlot();
          }
          lastClickTime = now;
          k_fire = false;
        }
        if (k_3 && cursor.count > 0) {
          placeOne();
          k_3 = false;
        }
        if (k_1 && cursor.count > 0) {
          if (!isDragging) {
            isDragging = true;
            dragSlots.removeAllElements();
          }
          addToDragSlots();
        } else if (isDragging) {
          distributeDrag();
          isDragging = false;
        }
      }
      if (k_pound || k_star) {
        closeInventory();
        k_pound = false;
        k_star = false;
        return;
      }
      Slot s = getCurrentSlot();
      if (s != null && s.count > 0) {
        String n = getItemName(s.id);
        if (!n.equals(tooltipName)) {
          tooltipName = n;
          tooltipTimer = 1000;
        }
      } else {
        tooltipName = "";
      }
    }

    private void moveCursor(int dx, int dy) {
      boolean wb = (gameState == 5), fur = (gameState == 6), ch = (gameState == 8);
      if (ch) {
        invCursorX += dx;
        if (invCursorX < 0) invCursorX = 8;
        if (invCursorX > 8) invCursorX = 0;
        if (invSection == SEC_HOTBAR) {
          if (dy < 0) {
            invSection = SEC_CHEST;
            invCursorY = chestRows - 1;
          }
        } else if (invSection == SEC_CHEST) {
          invCursorY += dy;
          if (invCursorY < 0) invCursorY = 0;
          if (invCursorY >= chestRows) {
            invSection = SEC_HOTBAR;
            invCursorY = 0;
          }
        }
        return;
      }
      int cW = wb ? 3 : 2, cH = wb ? 3 : 2;
      if (invSection == SEC_HOTBAR) {
        invCursorX += dx;
        if (invCursorX < 0) invCursorX = 8;
        else if (invCursorX > 8) {
          if (creativeMode) invSection = SEC_DELETE;
          else invCursorX = 0;
        }
        if (dy < 0) {
          if (creativeMode && creativeTab == 0 && !wb && !fur) {
            invSection = SEC_LIB;
            invCursorY = 7;
          } else {
            invSection = SEC_INV;
            invCursorY = 2;
          }
        }
      } else if (invSection == SEC_DELETE) {
        if (dx < 0) {
          invSection = SEC_HOTBAR;
          invCursorX = 8;
        }
        if (dy < 0) {
          invSection = SEC_INV;
          invCursorY = 2;
          invCursorX = 8;
        }
      } else if (invSection == SEC_INV) {
        invCursorX += dx;
        invCursorY += dy;
        if (invCursorX < 0) invCursorX = 8;
        else if (invCursorX > 8) invCursorX = 0;
        if (invCursorY > 2) {
          invSection = SEC_HOTBAR;
          invCursorY = 0;
        } else if (invCursorY < 0) {
          if (fur) {
            invSection = SEC_FURNACE_FUEL;
            invCursorX = 0;
          } else if (!wb && invCursorX <= 3) {
            invSection = SEC_ARMOR;
            invCursorY = 3;
            invCursorX = 0;
          } else {
            invSection = SEC_CRAFT;
            invCursorY = cH - 1;
            if (!wb) {
              if (invCursorX <= 5) invCursorX = 0;
              else invCursorX = 1;
            } else {
              if (invCursorX <= 2) invCursorX = 0;
              else if (invCursorX <= 5) invCursorX = 1;
              else invCursorX = 2;
            }
          }
        }
      } else if (invSection == SEC_ARMOR) {
        invCursorY += dy;
        if (invCursorY < 0) {
          if (creativeMode && !wb && !fur) {
            invSection = SEC_TABS;
            invCursorY = 0;
            creativeTab = 0;
          } else invCursorY = 0;
        }
        if (invCursorY > 3) {
          invSection = SEC_INV;
          invCursorY = 0;
          invCursorX = 0;
        }
        if (dx > 0) {
          invSection = SEC_CRAFT;
          invCursorX = 0;
          invCursorY = (invCursorY < 2) ? 0 : 1;
        }
      } else if (invSection == SEC_CRAFT) {
        invCursorX += dx;
        invCursorY += dy;
        if (invCursorX < 0) {
          if (!wb) {
            invSection = SEC_ARMOR;
            invCursorX = 0;
            invCursorY = (invCursorY == 0 ? 1 : 3);
          } else invCursorX = 0;
        }
        if (invCursorX >= cW) {
          invSection = SEC_RESULT;
          invCursorX = 0;
          invCursorY = 0;
        }
        if (invCursorY < 0) {
          if (creativeMode && !wb && !fur) {
            invSection = SEC_TABS;
            invCursorY = 0;
            creativeTab = 1;
          } else invCursorY = 0;
        }
        if (invCursorY >= cH) {
          invSection = SEC_INV;
          invCursorY = 0;
          invCursorX = 4;
        }
      } else if (invSection == SEC_RESULT) {
        if (dx < 0) {
          invSection = SEC_CRAFT;
          invCursorX = cW - 1;
          invCursorY = 0;
        }
        if (dy != 0) {
          invSection = SEC_INV;
          invCursorX = 8;
          invCursorY = 0;
        }
      } else if (invSection == SEC_FURNACE_IN) {
        if (dy > 0) invSection = SEC_FURNACE_FUEL;
        if (dx > 0) invSection = SEC_FURNACE_OUT;
      } else if (invSection == SEC_FURNACE_FUEL) {
        if (dy < 0) invSection = SEC_FURNACE_IN;
        if (dy > 0) {
          invSection = SEC_INV;
          invCursorY = 0;
          invCursorX = 4;
        }
        if (dx > 0) invSection = SEC_FURNACE_OUT;
      } else if (invSection == SEC_FURNACE_OUT) {
        if (dx < 0) invSection = SEC_FURNACE_FUEL;
        if (dy > 0) {
          invSection = SEC_INV;
          invCursorY = 0;
          invCursorX = 8;
        }
      }
    }

    private Slot getCurrentSlot() {
      if (invSection == SEC_HOTBAR) return hotbar[invCursorX];
      if (invSection == SEC_CHEST) {
        if (chestInv == null) return null;
        int idx = invCursorX + invCursorY * 9;
        if (idx < chestInv.length) return chestInv[idx];
        return null;
      }
      if (invSection == SEC_INV) return inventory[invCursorX + invCursorY * 9];
      if (invSection == SEC_ARMOR) return armor[invCursorY];
      if (invSection == SEC_CRAFT) {
        int w = (gameState == 5) ? 3 : 2;
        return craft[invCursorX + invCursorY * w];
      }
      if (invSection == SEC_RESULT) return craftResult;
      if (invSection == SEC_FURNACE_IN) return furnaceIn;
      if (invSection == SEC_FURNACE_FUEL) return furnaceFuel;
      if (invSection == SEC_FURNACE_OUT) return furnaceOut;
      if (invSection == SEC_LIB) {
        int idx = invCursorX + (invCursorY + libScroll) * 9;
        if (idx < libItems.length) {
          Slot s = new Slot();
          s.id = libItems[idx];
          s.count = 1;
          return s;
        }
      }
      return null;
    }

    private boolean isArmorItem(byte id) {
      return (id >= HELMET_IRON && id <= BOOTS_DIAMOND);
    }

    private boolean isArmorCorrectSlot(byte id, int slot) {
      if (slot == 0) return (id == HELMET_IRON || id == HELMET_GOLD || id == HELMET_DIAMOND);
      if (slot == 1)
        return (id == CHESTPLATE_IRON || id == CHESTPLATE_GOLD || id == CHESTPLATE_DIAMOND);
      if (slot == 2) return (id == LEGGINGS_IRON || id == LEGGINGS_GOLD || id == LEGGINGS_DIAMOND);
      if (slot == 3) return (id == BOOTS_IRON || id == BOOTS_GOLD || id == BOOTS_DIAMOND);
      return false;
    }

    private void clickSlot() {
      Slot slot = getCurrentSlot();
      if (slot == null) return;
      if (invSection == SEC_RESULT || invSection == SEC_FURNACE_OUT) {
        if (slot.count > 0) {
          if (cursor.count == 0 || (cursor.id == slot.id && cursor.count + slot.count <= 64)) {
            if (cursor.count == 0) {
              cursor.id = slot.id;
              cursor.count = slot.count;
            } else {
              cursor.count += slot.count;
            }
            if (invSection == SEC_RESULT) consumeCraft();
            else {
              slot.id = 0;
              slot.count = 0;
            }
            updateCrafting();
          }
        }
        return;
      }
      if (cursor.count == 0) {
        if (slot.count > 0) {
          cursor.id = slot.id;
          cursor.count = slot.count;
          slot.id = 0;
          slot.count = 0;
          updateCrafting();
        }
      } else {
        if (invSection == SEC_ARMOR) {
          if (!isArmorItem(cursor.id)) return;
          if (!isArmorCorrectSlot(cursor.id, invCursorY)) return;
        }
        if (slot.count == 0) {
          slot.id = cursor.id;
          slot.count = cursor.count;
          cursor.id = 0;
          cursor.count = 0;
          updateCrafting();
        } else if (slot.id == cursor.id) {
          int total = slot.count + cursor.count;
          if (total <= 64) {
            slot.count = total;
            cursor.id = 0;
            cursor.count = 0;
          } else {
            slot.count = 64;
            cursor.count = total - 64;
          }
          updateCrafting();
        } else {
          byte tmpId = slot.id;
          int tmpCount = slot.count;
          slot.id = cursor.id;
          slot.count = cursor.count;
          cursor.id = tmpId;
          cursor.count = tmpCount;
          updateCrafting();
        }
      }
    }

    private void placeOne() {
      Slot slot = getCurrentSlot();
      if (slot == null || invSection == SEC_RESULT || invSection == SEC_FURNACE_OUT) return;
      if (slot.count == 0) {
        slot.id = cursor.id;
        slot.count = 1;
        cursor.count--;
        if (cursor.count == 0) cursor.id = 0;
        updateCrafting();
      } else if (slot.id == cursor.id && slot.count < 64) {
        slot.count++;
        cursor.count--;
        if (cursor.count == 0) cursor.id = 0;
        updateCrafting();
      }
    }

    private void addToDragSlots() {
      int idx = getSlotIndex();
      if (idx == -1) return;
      for (int i = 0; i < dragSlots.size(); i++)
        if (((Integer) dragSlots.elementAt(i)).intValue() == idx) return;
      dragSlots.addElement(new Integer(idx));
    }

    private void distributeDrag() {
      int s = dragSlots.size();
      if (s == 0) return;
      int p = cursor.count / s, r = cursor.count % s;
      for (int i = 0; i < s; i++) {
        int idx = ((Integer) dragSlots.elementAt(i)).intValue();
        Slot slot = getSlotByIndex(idx);
        if (slot == null || (slot.count > 0 && slot.id != cursor.id)) continue;
        int g = p + (i < r ? 1 : 0);
        if (slot.count == 0) {
          slot.id = cursor.id;
          slot.count = g;
        } else {
          slot.count += g;
          if (slot.count > 64) slot.count = 64;
        }
        cursor.count -= g;
      }
      if (cursor.count <= 0) {
        cursor.id = 0;
        cursor.count = 0;
      }
      dragSlots.removeAllElements();
      updateCrafting();
    }

    private void collectStack() {
      if (cursor.count > 0) return;
      Slot slot = getCurrentSlot();
      if (slot == null
          || slot.count == 0
          || invSection == SEC_RESULT
          || invSection == SEC_FURNACE_OUT) return;
      byte t = slot.id;
      cursor.id = t;
      cursor.count = slot.count;
      slot.id = 0;
      slot.count = 0;
      int c = cursor.count;
      Slot[] all = new Slot[49];
      System.arraycopy(hotbar, 0, all, 0, 9);
      System.arraycopy(inventory, 0, all, 9, 27);
      System.arraycopy(armor, 0, all, 36, 4);
      System.arraycopy(craft, 0, all, 40, 9);
      for (int i = 0; i < 49 && c < 64; i++) {
        Slot s = all[i];
        if (s != null && s.count > 0 && s.id == t) {
          int take = Math.min(s.count, 64 - c);
          cursor.count += take;
          s.count -= take;
          if (s.count == 0) s.id = 0;
          c += take;
        }
      }
      updateCrafting();
    }

    private int getSlotIndex() {
      if (invSection == SEC_HOTBAR) return invCursorX;
      if (invSection == SEC_INV) return 9 + invCursorX + invCursorY * 9;
      return -1;
    }

    private Slot getSlotByIndex(int idx) {
      if (idx < 9) return hotbar[idx];
      if (idx < 36) return inventory[idx - 9];
      return null;
    }

    private void updateFurnace(int dt) {
      Enumeration e = tileEntities.elements();
      while (e.hasMoreElements()) {
        Object o = e.nextElement();
        if (o instanceof FurnaceTE) {
          FurnaceTE f = (FurnaceTE) o;
          if (f.bTime > 0) {
            f.bTime -= dt;
            if (f.bTime < 0) f.bTime = 0;
          }
          boolean can =
              (f.in.id != 0
                  && (f.out.count == 0
                      || (f.out.id == getSmeltResult(f.in.id) && f.out.count < 64)));
          if (can && f.in.id == 0) can = false;
          if (f.bTime == 0 && can) {
            int val = getFuelTime(f.fuel.id);
            if (val > 0) {
              f.bTime = val;
              f.bMax = val;
              f.fuel.count--;
              if (f.fuel.count == 0) f.fuel.id = 0;
            }
          }
          if (f.bTime > 0 && can) {
            f.cTime += dt;
            if (f.cTime >= 2000) {
              f.cTime = 0;
              byte r = getSmeltResult(f.in.id);
              if (r != 0) {
                f.in.count--;
                if (f.in.count == 0) f.in.id = 0;
                f.out.id = r;
                f.out.count++;
              }
            }
          } else {
            if (f.cTime > 0) f.cTime -= dt * 2;
            if (f.cTime < 0) f.cTime = 0;
          }
        }
      }
      if (gameState == 6 && hasTarget) {
        FurnaceTE f = getFurnaceAt(targetX, targetY, targetZ);
        if (f != null) {
          burnTime = f.bTime;
          burnTimeMax = f.bMax;
          cookTime = f.cTime;
          furnaceIn = f.in;
          furnaceFuel = f.fuel;
          furnaceOut = f.out;
        }
      }
    }

    private byte getSmeltResult(byte id) {
      if (id == SAND) return GLASS;
      if (id == WOOD) return CHARCOAL;
      if (id == ORE_IRON) return IRON_INGOT;
      if (id == ORE_GOLD) return GOLD_INGOT;
      if (id == COBBLE) return STONE;
      return 0;
    }

    private boolean canSmelt() {
      if (furnaceIn.id == COBBLE)
        return (furnaceOut.count == 0 || (furnaceOut.id == STONE && furnaceOut.count < 64));
      if (furnaceIn.id == SAND)
        return (furnaceOut.count == 0 || (furnaceOut.id == GLASS && furnaceOut.count < 64));
      if (furnaceIn.id == WOOD)
        return (furnaceOut.count == 0 || (furnaceOut.id == CHARCOAL && furnaceOut.count < 64));
      if (furnaceIn.id == ORE_IRON)
        return (furnaceOut.count == 0 || (furnaceOut.id == IRON_INGOT && furnaceOut.count < 64));
      if (furnaceIn.id == ORE_GOLD)
        return (furnaceOut.count == 0 || (furnaceOut.id == GOLD_INGOT && furnaceOut.count < 64));
      return false;
    }

    private void smeltItem() {
      if (!canSmelt()) return;
      byte res = STONE;
      if (furnaceIn.id == SAND) res = GLASS;
      else if (furnaceIn.id == WOOD) res = CHARCOAL;
      else if (furnaceIn.id == ORE_IRON) res = IRON_INGOT;
      else if (furnaceIn.id == ORE_GOLD) res = GOLD_INGOT;
      furnaceIn.count--;
      if (furnaceIn.count <= 0) furnaceIn.id = 0;
      furnaceOut.id = res;
      furnaceOut.count++;
    }

    private int getFuelTime(byte id) {
      if (id == WOOD || id == PLANKS || id == WORKBENCH) return 5000;
      if (id == STICK) return 1000;
      if (id >= WOOD_PICKAXE && id <= WOOD_SWORD) return 5000;
      if (id == COAL || id == CHARCOAL) return 16000;
      return 0;
    }

    private boolean isPlank(byte id) {
      return id == PLANKS
          || id == PLANKS_BIRCH
          || id == PLANKS_SPRUCE
          || id == PLANKS_JUNGLE
          || id == PLANKS_ACACIA
          || id == PLANKS_DARK_OAK;
    }

    private boolean ckPlanks(byte[] g, int w, int x, int y, int c) {
      for (int i = 0; i < c; i++) if (!isPlank(ggi(g, w, x + i, y))) return false;
      return true;
    }

    private boolean ckPlankStick(byte[] g, int w, int x, int y) {
      return isPlank(ggi(g, w, x, y)) && ggi(g, w, x + 1, y) == STICK;
    }

    private void updateCrafting() {
      if (gameState != 6) {
        boolean wb = (gameState == 5);
        int w = wb ? 3 : 2;
        int minX_t = w, minY_t = 10, maxX_t = -1, maxY_t = -1;
        int c_count = 0;
        for (int i = 0; i < craft.length; i++) if (craft[i].count > 0) c_count++;
        if (c_count == 2) {
          byte[] g_t = new byte[craft.length];
          for (int i = 0; i < craft.length; i++) g_t[i] = (craft[i].count > 0) ? craft[i].id : 0;
          for (int y = 0; y < (wb ? 3 : 2); y++)
            for (int x = 0; x < w; x++) {
              if (g_t[x + y * w] != 0) {
                if (x < minX_t) minX_t = x;
                if (x > maxX_t) maxX_t = x;
                if (y < minY_t) minY_t = y;
                if (y > maxY_t) maxY_t = y;
              }
            }
          int rw_t = maxX_t - minX_t + 1;
          int rh_t = maxY_t - minY_t + 1;
          if (rw_t == 1 && rh_t == 2) {
            byte top = g_t[minX_t + minY_t * w];
            byte bot = g_t[minX_t + (minY_t + 1) * w];
            if ((top == COAL || top == CHARCOAL) && bot == STICK) {
              craftResult.id = TORCH;
              craftResult.count = 4;
              return;
            }
          }
        }
      }
      if (gameState == 6) return;
      boolean wb = (gameState == 5);
      int w = wb ? 3 : 2, h = wb ? 3 : 2;
      craftResult.id = 0;
      craftResult.count = 0;
      byte[] g = new byte[w * h];
      for (int i = 0; i < w * h; i++) g[i] = (craft[i].count > 0) ? craft[i].id : 0;
      int minX = w, minY = h, maxX = -1, maxY = -1;
      boolean e = true;
      for (int y = 0; y < h; y++)
        for (int x = 0; x < w; x++) {
          if (g[x + y * w] != 0) {
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
            e = false;
          }
        }
      if (e) return;
      int rw = maxX - minX + 1, rh = maxY - minY + 1;
      if (rw == 3
          && rh == 2
          && ck(g, w, minX, minY, WOOL_RED, WOOL_RED, WOOL_RED)
          && ck(g, w, minX, minY + 1, PLANKS, PLANKS, PLANKS)) {
        craftResult.id = BED_BLOCK;
        craftResult.count = 1;
        return;
      }
      if (rw == 3
          && rh == 3
          && ck(g, w, minX, minY, STICK, AIR, STICK)
          && ck(g, w, minX, minY + 1, STICK, STICK, STICK)
          && ck(g, w, minX, minY + 2, STICK, AIR, STICK)) {
        craftResult.id = LADDER;
        craftResult.count = 3;
        return;
      }
      if (rw == 2 && rh == 1 && ck2(g, w, minX, minY, STONE, STONE)) {
        craftResult.id = PLATE_STONE;
        craftResult.count = 1;
        return;
      }
      if (rw == 2 && rh == 1 && ck2(g, w, minX, minY, GOLD_INGOT, GOLD_INGOT)) {
        craftResult.id = PLATE_GOLD;
        craftResult.count = 1;
        return;
      }
      if (rw == 2 && rh == 1 && ck2(g, w, minX, minY, IRON_INGOT, IRON_INGOT)) {
        craftResult.id = PLATE_IRON;
        craftResult.count = 1;
        return;
      }
      if (rw == 2 && rh == 1 && ck2(g, w, minX, minY, PLANKS, PLANKS)) {
        craftResult.id = PLATE_OAK;
        craftResult.count = 1;
        return;
      }
      if (rw == 2 && rh == 1 && ck2(g, w, minX, minY, WOOL_WHITE, WOOL_WHITE)) {
        craftResult.id = CARPET_WHITE;
        craftResult.count = 3;
        return;
      }
      if (rw == 2 && rh == 1 && ck2(g, w, minX, minY, WOOL_ORANGE, WOOL_ORANGE)) {
        craftResult.id = CARPET_ORANGE;
        craftResult.count = 3;
        return;
      }
      if (rw == 2 && rh == 1 && ck2(g, w, minX, minY, WOOL_MAGENTA, WOOL_MAGENTA)) {
        craftResult.id = CARPET_MAGENTA;
        craftResult.count = 3;
        return;
      }
      if (rw == 2 && rh == 1 && ck2(g, w, minX, minY, WOOL_LIGHT_BLUE, WOOL_LIGHT_BLUE)) {
        craftResult.id = CARPET_LIGHT_BLUE;
        craftResult.count = 3;
        return;
      }
      if (rw == 2 && rh == 1 && ck2(g, w, minX, minY, WOOL_YELLOW, WOOL_YELLOW)) {
        craftResult.id = CARPET_YELLOW;
        craftResult.count = 3;
        return;
      }
      if (rw == 2 && rh == 1 && ck2(g, w, minX, minY, WOOL_LIME, WOOL_LIME)) {
        craftResult.id = CARPET_LIME;
        craftResult.count = 3;
        return;
      }
      if (rw == 2 && rh == 1 && ck2(g, w, minX, minY, WOOL_PINK, WOOL_PINK)) {
        craftResult.id = CARPET_PINK;
        craftResult.count = 3;
        return;
      }
      if (rw == 2 && rh == 1 && ck2(g, w, minX, minY, WOOL_GRAY, WOOL_GRAY)) {
        craftResult.id = CARPET_GRAY;
        craftResult.count = 3;
        return;
      }
      if (rw == 2 && rh == 1 && ck2(g, w, minX, minY, WOOL_LIGHT_GRAY, WOOL_LIGHT_GRAY)) {
        craftResult.id = CARPET_LIGHT_GRAY;
        craftResult.count = 3;
        return;
      }
      if (rw == 2 && rh == 1 && ck2(g, w, minX, minY, WOOL_CYAN, WOOL_CYAN)) {
        craftResult.id = CARPET_CYAN;
        craftResult.count = 3;
        return;
      }
      if (rw == 2 && rh == 1 && ck2(g, w, minX, minY, WOOL_PURPLE, WOOL_PURPLE)) {
        craftResult.id = CARPET_PURPLE;
        craftResult.count = 3;
        return;
      }
      if (rw == 2 && rh == 1 && ck2(g, w, minX, minY, WOOL_BLUE, WOOL_BLUE)) {
        craftResult.id = CARPET_BLUE;
        craftResult.count = 3;
        return;
      }
      if (rw == 2 && rh == 1 && ck2(g, w, minX, minY, WOOL_BROWN, WOOL_BROWN)) {
        craftResult.id = CARPET_BROWN;
        craftResult.count = 3;
        return;
      }
      if (rw == 2 && rh == 1 && ck2(g, w, minX, minY, WOOL_GREEN, WOOL_GREEN)) {
        craftResult.id = CARPET_GREEN;
        craftResult.count = 3;
        return;
      }
      if (rw == 2 && rh == 1 && ck2(g, w, minX, minY, WOOL_RED, WOOL_RED)) {
        craftResult.id = CARPET_RED;
        craftResult.count = 3;
        return;
      }
      if (rw == 2 && rh == 1 && ck2(g, w, minX, minY, WOOL_BLACK, WOOL_BLACK)) {
        craftResult.id = CARPET_BLACK;
        craftResult.count = 3;
        return;
      }
      if (rw == 3 && rh == 1 && ck(g, w, minX, minY, COBBLE, COBBLE, COBBLE)) {
        craftResult.id = SLAB_COBBLE;
        craftResult.count = 6;
        return;
      }
      if (rw == 3 && rh == 1 && ck(g, w, minX, minY, PLANKS, PLANKS, PLANKS)) {
        craftResult.id = SLAB_OAK;
        craftResult.count = 6;
        return;
      }
      if (rw == 3
          && rh == 2
          && ck(g, w, minX, minY, IRON_INGOT, IRON_INGOT, IRON_INGOT)
          && ck(g, w, minX, minY + 1, IRON_INGOT, IRON_INGOT, IRON_INGOT)) {
        craftResult.id = IRON_BARS;
        craftResult.count = 16;
        return;
      }
      if (rw == 3
          && rh == 2
          && ck(g, w, minX, minY, GLASS, GLASS, GLASS)
          && ck(g, w, minX, minY + 1, GLASS, GLASS, GLASS)) {
        craftResult.id = GLASS_PANE;
        craftResult.count = 16;
        return;
      }
      if (rw == 3
          && rh == 3
          && isPlank(ggi(g, w, minX, minY))
          && ggi(g, w, minX + 1, minY) == AIR
          && ggi(g, w, minX + 2, minY) == AIR
          && isPlank(ggi(g, w, minX, minY + 1))
          && isPlank(ggi(g, w, minX + 1, minY + 1))
          && ggi(g, w, minX + 2, minY + 1) == AIR
          && isPlank(ggi(g, w, minX, minY + 2))
          && isPlank(ggi(g, w, minX + 1, minY + 2))
          && isPlank(ggi(g, w, minX + 2, minY + 2))) {
        craftResult.id = STAIRS_WOOD;
        craftResult.count = 4;
        return;
      }
      if (rw == 3
          && rh == 3
          && ggi(g, w, minX, minY) == AIR
          && ggi(g, w, minX + 1, minY) == AIR
          && isPlank(ggi(g, w, minX + 2, minY))
          && ggi(g, w, minX, minY + 1) == AIR
          && isPlank(ggi(g, w, minX + 1, minY + 1))
          && isPlank(ggi(g, w, minX + 2, minY + 1))
          && isPlank(ggi(g, w, minX, minY + 2))
          && isPlank(ggi(g, w, minX + 1, minY + 2))
          && isPlank(ggi(g, w, minX + 2, minY + 2))) {
        craftResult.id = STAIRS_WOOD;
        craftResult.count = 4;
        return;
      }
      if (rw == 3
          && rh == 3
          && ggi(g, w, minX, minY) == COBBLE
          && ggi(g, w, minX + 1, minY) == AIR
          && ggi(g, w, minX + 2, minY) == AIR
          && ggi(g, w, minX, minY + 1) == COBBLE
          && ggi(g, w, minX + 1, minY + 1) == COBBLE
          && ggi(g, w, minX + 2, minY + 1) == AIR
          && ggi(g, w, minX, minY + 2) == COBBLE
          && ggi(g, w, minX + 1, minY + 2) == COBBLE
          && ggi(g, w, minX + 2, minY + 2) == COBBLE) {
        craftResult.id = STAIRS_COBBLE;
        craftResult.count = 4;
        return;
      }
      if (rw == 3
          && rh == 3
          && ggi(g, w, minX, minY) == AIR
          && ggi(g, w, minX + 1, minY) == AIR
          && ggi(g, w, minX + 2, minY) == COBBLE
          && ggi(g, w, minX, minY + 1) == AIR
          && ggi(g, w, minX + 1, minY + 1) == COBBLE
          && ggi(g, w, minX + 2, minY + 1) == COBBLE
          && ggi(g, w, minX, minY + 2) == COBBLE
          && ggi(g, w, minX + 1, minY + 2) == COBBLE
          && ggi(g, w, minX + 2, minY + 2) == COBBLE) {
        craftResult.id = STAIRS_COBBLE;
        craftResult.count = 4;
        return;
      }
      if (rw == 3
          && rh == 2
          && isPlank(ggi(g, w, minX, minY))
          && ggi(g, w, minX + 1, minY) == STICK
          && isPlank(ggi(g, w, minX + 2, minY))
          && isPlank(ggi(g, w, minX, minY + 1))
          && ggi(g, w, minX + 1, minY + 1) == STICK
          && isPlank(ggi(g, w, minX + 2, minY + 1))) {
        craftResult.id = FENCE;
        craftResult.count = 3;
        return;
      }
      if (rw == 3
          && rh == 2
          && ckPlanks(g, w, minX, minY, 3)
          && ckPlanks(g, w, minX, minY + 1, 3)) {
        craftResult.id = BOOKSHELF;
        craftResult.count = 1;
        return;
      }
      if (rw == 3 && rh == 3 && !wb) {
        boolean isChest = true;
        for (int y = 0; y < 3; y++)
          for (int x = 0; x < 3; x++) {
            if (x == 1 && y == 1) {
              if (ggi(g, w, minX + x, minY + y) != AIR) isChest = false;
            } else {
              if (!isPlank(ggi(g, w, minX + x, minY + y))) isChest = false;
            }
          }
        if (isChest) {
          craftResult.id = CHEST;
          craftResult.count = 1;
          return;
        }
      }
      if (rw == 1 && rh == 1 && ggi(g, w, minX, minY) == WOOD) {
        craftResult.id = PLANKS;
        craftResult.count = 4;
        return;
      }
      if (rw == 1 && rh == 1 && ggi(g, w, minX, minY) == WOOD_BIRCH) {
        craftResult.id = PLANKS_BIRCH;
        craftResult.count = 4;
        return;
      }
      if (rw == 1 && rh == 1 && ggi(g, w, minX, minY) == WOOD_SPRUCE) {
        craftResult.id = PLANKS_SPRUCE;
        craftResult.count = 4;
        return;
      }
      if (rw == 1 && rh == 1 && ggi(g, w, minX, minY) == WOOD_JUNGLE) {
        craftResult.id = PLANKS_JUNGLE;
        craftResult.count = 4;
        return;
      }
      if (rw == 1 && rh == 1 && ggi(g, w, minX, minY) == WOOD_ACACIA) {
        craftResult.id = PLANKS_ACACIA;
        craftResult.count = 4;
        return;
      }
      if (rw == 1 && rh == 1 && ggi(g, w, minX, minY) == WOOD_DARK_OAK) {
        craftResult.id = PLANKS_DARK_OAK;
        craftResult.count = 4;
        return;
      }
      if (rw == 1 && rh == 2) {
        if (isPlank(ggi(g, w, minX, minY)) && isPlank(ggi(g, w, minX, minY + 1))) {
          craftResult.id = STICK;
          craftResult.count = 4;
          return;
        }
      }
      if (rw == 3 && rh == 1 && ck(g, w, minX, minY, WHEAT, WHEAT, WHEAT)) {
        craftResult.id = BREAD;
        craftResult.count = 1;
        return;
      }
      if (rw == 2 && rh == 2) {
        boolean ap = true;
        for (int y = 0; y < 2; y++)
          for (int x = 0; x < 2; x++) if (!isPlank(ggi(g, w, minX + x, minY + y))) ap = false;
        if (ap) {
          craftResult.id = WORKBENCH;
          craftResult.count = 1;
          return;
        }
      }
      if (rw == 2 && rh == 2) {
        if (ggi(g, w, minX, minY) == IRON_INGOT
            && ggi(g, w, minX + 1, minY) == AIR
            && ggi(g, w, minX, minY + 1) == AIR
            && ggi(g, w, minX + 1, minY + 1) == FLINT) {
          craftResult.id = FLINT_AND_STEEL;
          craftResult.count = 1;
          return;
        }
        if (ggi(g, w, minX + 1, minY) == IRON_INGOT
            && ggi(g, w, minX, minY) == AIR
            && ggi(g, w, minX + 1, minY + 1) == AIR
            && ggi(g, w, minX, minY + 1) == FLINT) {
          craftResult.id = FLINT_AND_STEEL;
          craftResult.count = 1;
          return;
        }
        if (ck2(g, w, minX, minY, SAND, SAND) && ck2(g, w, minX, minY + 1, SAND, SAND)) {
          craftResult.id = SANDSTONE;
          craftResult.count = 1;
          return;
        }
        if (ck2(g, w, minX, minY, GLOWSTONE_DUST, GLOWSTONE_DUST)
            && ck2(g, w, minX, minY + 1, GLOWSTONE_DUST, GLOWSTONE_DUST)) {
          craftResult.id = GLOWSTONE;
          craftResult.count = 1;
          return;
        }
        if (ck2(g, w, minX, minY, SNOW_LAYER, SNOW_LAYER)
            && ck2(g, w, minX, minY + 1, SNOW_LAYER, SNOW_LAYER)) {
          craftResult.id = SNOW_BLOCK;
          craftResult.count = 1;
          return;
        }
      }
      if (rw == 2 && rh == 2) {
        if (ck2(g, w, minX, minY, QUARTZ, QUARTZ) && ck2(g, w, minX, minY + 1, QUARTZ, QUARTZ)) {
          craftResult.id = BLOCK_QUARTZ;
          craftResult.count = 1;
          return;
        }
        if (ck2(g, w, minX, minY, IRON_INGOT, AIR) && ck2(g, w, minX, minY + 1, AIR, IRON_INGOT)) {
          craftResult.id = SHEARS;
          craftResult.count = 1;
          return;
        }
        if (ck2(g, w, minX, minY, AIR, IRON_INGOT) && ck2(g, w, minX, minY + 1, IRON_INGOT, AIR)) {
          craftResult.id = SHEARS;
          craftResult.count = 1;
          return;
        }
      }
      if (rw == 3 && rh == 3 && wb) {
        if (ck(g, w, minX, minY, COAL, COAL, COAL)
            && ck(g, w, minX, minY + 1, COAL, COAL, COAL)
            && ck(g, w, minX, minY + 2, COAL, COAL, COAL)) {
          craftResult.id = BLOCK_COAL;
          craftResult.count = 1;
          return;
        }
        if (ck(g, w, minX, minY, IRON_INGOT, IRON_INGOT, IRON_INGOT)
            && ck(g, w, minX, minY + 1, IRON_INGOT, IRON_INGOT, IRON_INGOT)
            && ck(g, w, minX, minY + 2, IRON_INGOT, IRON_INGOT, IRON_INGOT)) {
          craftResult.id = BLOCK_IRON;
          craftResult.count = 1;
          return;
        }
        if (ck(g, w, minX, minY, GOLD_INGOT, GOLD_INGOT, GOLD_INGOT)
            && ck(g, w, minX, minY + 1, GOLD_INGOT, GOLD_INGOT, GOLD_INGOT)
            && ck(g, w, minX, minY + 2, GOLD_INGOT, GOLD_INGOT, GOLD_INGOT)) {
          craftResult.id = BLOCK_GOLD;
          craftResult.count = 1;
          return;
        }
        if (ck(g, w, minX, minY, REDSTONE, REDSTONE, REDSTONE)
            && ck(g, w, minX, minY + 1, REDSTONE, REDSTONE, REDSTONE)
            && ck(g, w, minX, minY + 2, REDSTONE, REDSTONE, REDSTONE)) {
          craftResult.id = BLOCK_REDSTONE;
          craftResult.count = 1;
          return;
        }
        if (ck(g, w, minX, minY, EMERALD, EMERALD, EMERALD)
            && ck(g, w, minX, minY + 1, EMERALD, EMERALD, EMERALD)
            && ck(g, w, minX, minY + 2, EMERALD, EMERALD, EMERALD)) {
          craftResult.id = BLOCK_EMERALD;
          craftResult.count = 1;
          return;
        }
        if (ck(g, w, minX, minY, LAPIS, LAPIS, LAPIS)
            && ck(g, w, minX, minY + 1, LAPIS, LAPIS, LAPIS)
            && ck(g, w, minX, minY + 2, LAPIS, LAPIS, LAPIS)) {
          craftResult.id = BLOCK_LAPIS;
          craftResult.count = 1;
          return;
        }
        if (ck(g, w, minX, minY, DIAMOND, DIAMOND, DIAMOND)
            && ck(g, w, minX, minY + 1, DIAMOND, DIAMOND, DIAMOND)
            && ck(g, w, minX, minY + 2, DIAMOND, DIAMOND, DIAMOND)) {
          craftResult.id = BLOCK_DIAMOND;
          craftResult.count = 1;
          return;
        }
        boolean r = true;
        for (int y = 0; y < 3; y++)
          for (int x = 0; x < 3; x++) {
            if (x == 1 && y == 1) {
              if (ggi(g, w, minX + x, minY + y) != AIR) r = false;
            } else {
              if (ggi(g, w, minX + x, minY + y) != COBBLE) r = false;
            }
          }
        if (r) {
          craftResult.id = FURNACE;
          craftResult.count = 1;
          return;
        }
      }
      if (rh == 3) {
        if (wb
            && rw == 2
            && ckPlanks(g, w, minX, minY, 2)
            && ckPlanks(g, w, minX, minY + 1, 2)
            && ckPlanks(g, w, minX, minY + 2, 2)) {
          craftResult.id = WOOD_DOOR;
          craftResult.count = 3;
          return;
        }
        if (rw == 3
            && ck(g, w, minX, minY, COBBLE, COBBLE, COBBLE)
            && ck(g, w, minX, minY + 1, AIR, STICK, AIR)
            && ck(g, w, minX, minY + 2, AIR, STICK, AIR)) {
          craftResult.id = STONE_PICKAXE;
          craftResult.count = 1;
          return;
        }
        if (rw == 2
            && ck2(g, w, minX, minY, COBBLE, COBBLE)
            && ck2(g, w, minX, minY + 1, COBBLE, STICK)
            && ck2(g, w, minX, minY + 2, AIR, STICK)) {
          craftResult.id = STONE_AXE;
          craftResult.count = 1;
          return;
        }
        if (rw == 1
            && ggi(g, w, minX, minY) == COBBLE
            && ggi(g, w, minX, minY + 1) == STICK
            && ggi(g, w, minX, minY + 2) == STICK) {
          craftResult.id = STONE_SHOVEL;
          craftResult.count = 1;
          return;
        }
        if (rw == 1
            && ggi(g, w, minX, minY) == COBBLE
            && ggi(g, w, minX, minY + 1) == COBBLE
            && ggi(g, w, minX, minY + 2) == STICK) {
          craftResult.id = STONE_SWORD;
          craftResult.count = 1;
          return;
        }
        if (rw == 2
            && ck2(g, w, minX, minY, COBBLE, COBBLE)
            && (ck2(g, w, minX, minY + 1, AIR, STICK) || ck2(g, w, minX, minY + 1, STICK, AIR))
            && (ck2(g, w, minX, minY + 2, AIR, STICK) || ck2(g, w, minX, minY + 2, STICK, AIR))) {
          craftResult.id = STONE_HOE;
          craftResult.count = 1;
          return;
        }
        if (rw == 3
            && ckPlanks(g, w, minX, minY, 3)
            && ck(g, w, minX, minY + 1, AIR, STICK, AIR)
            && ck(g, w, minX, minY + 2, AIR, STICK, AIR)) {
          craftResult.id = WOOD_PICKAXE;
          craftResult.count = 1;
          return;
        }
        if (rw == 2
            && ckPlanks(g, w, minX, minY, 2)
            && ckPlankStick(g, w, minX, minY + 1)
            && ck2(g, w, minX, minY + 2, AIR, STICK)) {
          craftResult.id = WOOD_AXE;
          craftResult.count = 1;
          return;
        }
        if (rw == 1
            && isPlank(ggi(g, w, minX, minY))
            && ggi(g, w, minX, minY + 1) == STICK
            && ggi(g, w, minX, minY + 2) == STICK) {
          craftResult.id = WOOD_SHOVEL;
          craftResult.count = 1;
          return;
        }
        if (rw == 1
            && isPlank(ggi(g, w, minX, minY))
            && isPlank(ggi(g, w, minX, minY + 1))
            && ggi(g, w, minX, minY + 2) == STICK) {
          craftResult.id = WOOD_SWORD;
          craftResult.count = 1;
          return;
        }
        if (rw == 3
            && ck(g, w, minX, minY, IRON_INGOT, IRON_INGOT, IRON_INGOT)
            && ck(g, w, minX, minY + 1, AIR, STICK, AIR)
            && ck(g, w, minX, minY + 2, AIR, STICK, AIR)) {
          craftResult.id = IRON_PICKAXE;
          craftResult.count = 1;
          return;
        }
        if (rw == 2
            && ck2(g, w, minX, minY, IRON_INGOT, IRON_INGOT)
            && ck2(g, w, minX, minY + 1, IRON_INGOT, STICK)
            && ck2(g, w, minX, minY + 2, AIR, STICK)) {
          craftResult.id = IRON_AXE;
          craftResult.count = 1;
          return;
        }
        if (rw == 1
            && ggi(g, w, minX, minY) == IRON_INGOT
            && ggi(g, w, minX, minY + 1) == STICK
            && ggi(g, w, minX, minY + 2) == STICK) {
          craftResult.id = IRON_SHOVEL;
          craftResult.count = 1;
          return;
        }
        if (rw == 1
            && ggi(g, w, minX, minY) == IRON_INGOT
            && ggi(g, w, minX, minY + 1) == IRON_INGOT
            && ggi(g, w, minX, minY + 2) == STICK) {
          craftResult.id = IRON_SWORD;
          craftResult.count = 1;
          return;
        }
        if (rw == 3
            && ck(g, w, minX, minY, GOLD_INGOT, GOLD_INGOT, GOLD_INGOT)
            && ck(g, w, minX, minY + 1, AIR, STICK, AIR)
            && ck(g, w, minX, minY + 2, AIR, STICK, AIR)) {
          craftResult.id = GOLD_PICKAXE;
          craftResult.count = 1;
          return;
        }
        if (rw == 2
            && ck2(g, w, minX, minY, GOLD_INGOT, GOLD_INGOT)
            && ck2(g, w, minX, minY + 1, GOLD_INGOT, STICK)
            && ck2(g, w, minX, minY + 2, AIR, STICK)) {
          craftResult.id = GOLD_AXE;
          craftResult.count = 1;
          return;
        }
        if (rw == 1
            && ggi(g, w, minX, minY) == GOLD_INGOT
            && ggi(g, w, minX, minY + 1) == STICK
            && ggi(g, w, minX, minY + 2) == STICK) {
          craftResult.id = GOLD_SHOVEL;
          craftResult.count = 1;
          return;
        }
        if (rw == 1
            && ggi(g, w, minX, minY) == GOLD_INGOT
            && ggi(g, w, minX, minY + 1) == GOLD_INGOT
            && ggi(g, w, minX, minY + 2) == STICK) {
          craftResult.id = GOLD_SWORD;
          craftResult.count = 1;
          return;
        }
        if (rw == 3
            && ck(g, w, minX, minY, DIAMOND, DIAMOND, DIAMOND)
            && ck(g, w, minX, minY + 1, AIR, STICK, AIR)
            && ck(g, w, minX, minY + 2, AIR, STICK, AIR)) {
          craftResult.id = DIAMOND_PICKAXE;
          craftResult.count = 1;
          return;
        }
        if (rw == 2
            && ck2(g, w, minX, minY, DIAMOND, DIAMOND)
            && ck2(g, w, minX, minY + 1, DIAMOND, STICK)
            && ck2(g, w, minX, minY + 2, AIR, STICK)) {
          craftResult.id = DIAMOND_AXE;
          craftResult.count = 1;
          return;
        }
        if (rw == 1
            && ggi(g, w, minX, minY) == DIAMOND
            && ggi(g, w, minX, minY + 1) == STICK
            && ggi(g, w, minX, minY + 2) == STICK) {
          craftResult.id = DIAMOND_SHOVEL;
          craftResult.count = 1;
          return;
        }
        if (rw == 1
            && ggi(g, w, minX, minY) == DIAMOND
            && ggi(g, w, minX, minY + 1) == DIAMOND
            && ggi(g, w, minX, minY + 2) == STICK) {
          craftResult.id = DIAMOND_SWORD;
          craftResult.count = 1;
          return;
        }
        if (rw == 2) {
          if (ckPlanks(g, w, minX, minY, 2)
              && (ck2(g, w, minX, minY + 1, AIR, STICK) || ck2(g, w, minX, minY + 1, STICK, AIR))
              && (ck2(g, w, minX, minY + 2, AIR, STICK) || ck2(g, w, minX, minY + 2, STICK, AIR))) {
            craftResult.id = WOOD_HOE;
            craftResult.count = 1;
            return;
          }
          if (ck2(g, w, minX, minY, IRON_INGOT, IRON_INGOT)
              && (ck2(g, w, minX, minY + 1, AIR, STICK) || ck2(g, w, minX, minY + 1, STICK, AIR))
              && (ck2(g, w, minX, minY + 2, AIR, STICK) || ck2(g, w, minX, minY + 2, STICK, AIR))) {
            craftResult.id = IRON_HOE;
            craftResult.count = 1;
            return;
          }
          if (ck2(g, w, minX, minY, GOLD_INGOT, GOLD_INGOT)
              && (ck2(g, w, minX, minY + 1, AIR, STICK) || ck2(g, w, minX, minY + 1, STICK, AIR))
              && (ck2(g, w, minX, minY + 2, AIR, STICK) || ck2(g, w, minX, minY + 2, STICK, AIR))) {
            craftResult.id = GOLD_HOE;
            craftResult.count = 1;
            return;
          }
          if (ck2(g, w, minX, minY, DIAMOND, DIAMOND)
              && (ck2(g, w, minX, minY + 1, AIR, STICK) || ck2(g, w, minX, minY + 1, STICK, AIR))
              && (ck2(g, w, minX, minY + 2, AIR, STICK) || ck2(g, w, minX, minY + 2, STICK, AIR))) {
            craftResult.id = DIAMOND_HOE;
            craftResult.count = 1;
            return;
          }
        }
        if (wb) {
          if (rw == 3
              && rh == 2
              && ck(g, w, minX, minY, IRON_INGOT, IRON_INGOT, IRON_INGOT)
              && ck(g, w, minX, minY + 1, IRON_INGOT, AIR, IRON_INGOT)) {
            craftResult.id = HELMET_IRON;
            craftResult.count = 1;
            return;
          }
          if (rw == 3
              && rh == 3
              && ck(g, w, minX, minY, IRON_INGOT, AIR, IRON_INGOT)
              && ck(g, w, minX, minY + 1, IRON_INGOT, IRON_INGOT, IRON_INGOT)
              && ck(g, w, minX, minY + 2, IRON_INGOT, IRON_INGOT, IRON_INGOT)) {
            craftResult.id = CHESTPLATE_IRON;
            craftResult.count = 1;
            return;
          }
          if (rw == 3
              && rh == 3
              && ck(g, w, minX, minY, IRON_INGOT, IRON_INGOT, IRON_INGOT)
              && ck(g, w, minX, minY + 1, IRON_INGOT, AIR, IRON_INGOT)
              && ck(g, w, minX, minY + 2, IRON_INGOT, AIR, IRON_INGOT)) {
            craftResult.id = LEGGINGS_IRON;
            craftResult.count = 1;
            return;
          }
          if (rw == 2
              && rh == 2
              && ck2(g, w, minX, minY, IRON_INGOT, AIR)
              && ck2(g, w, minX, minY + 1, IRON_INGOT, IRON_INGOT)) {
            craftResult.id = BOOTS_IRON;
            craftResult.count = 1;
            return;
          }
          if (rw == 3
              && rh == 2
              && ck(g, w, minX, minY, IRON_INGOT, AIR, IRON_INGOT)
              && ck(g, w, minX, minY + 1, AIR, IRON_INGOT, AIR)) {
            craftResult.id = BUCKET;
            craftResult.count = 1;
            return;
          }
          if (rw == 3
              && rh == 2
              && ck(g, w, minX, minY, GOLD_INGOT, GOLD_INGOT, GOLD_INGOT)
              && ck(g, w, minX, minY + 1, GOLD_INGOT, AIR, GOLD_INGOT)) {
            craftResult.id = HELMET_GOLD;
            craftResult.count = 1;
            return;
          }
          if (rw == 3
              && rh == 3
              && ck(g, w, minX, minY, GOLD_INGOT, AIR, GOLD_INGOT)
              && ck(g, w, minX, minY + 1, GOLD_INGOT, GOLD_INGOT, GOLD_INGOT)
              && ck(g, w, minX, minY + 2, GOLD_INGOT, GOLD_INGOT, GOLD_INGOT)) {
            craftResult.id = CHESTPLATE_GOLD;
            craftResult.count = 1;
            return;
          }
          if (rw == 3
              && rh == 3
              && ck(g, w, minX, minY, GOLD_INGOT, GOLD_INGOT, GOLD_INGOT)
              && ck(g, w, minX, minY + 1, GOLD_INGOT, AIR, GOLD_INGOT)
              && ck(g, w, minX, minY + 2, GOLD_INGOT, AIR, GOLD_INGOT)) {
            craftResult.id = LEGGINGS_GOLD;
            craftResult.count = 1;
            return;
          }
          if (rw == 2
              && rh == 2
              && ck2(g, w, minX, minY, GOLD_INGOT, AIR)
              && ck2(g, w, minX, minY + 1, GOLD_INGOT, GOLD_INGOT)) {
            craftResult.id = BOOTS_GOLD;
            craftResult.count = 1;
            return;
          }
          if (rw == 3
              && rh == 2
              && ck(g, w, minX, minY, DIAMOND, DIAMOND, DIAMOND)
              && ck(g, w, minX, minY + 1, DIAMOND, AIR, DIAMOND)) {
            craftResult.id = HELMET_DIAMOND;
            craftResult.count = 1;
            return;
          }
          if (rw == 3
              && rh == 3
              && ck(g, w, minX, minY, DIAMOND, AIR, DIAMOND)
              && ck(g, w, minX, minY + 1, DIAMOND, DIAMOND, DIAMOND)
              && ck(g, w, minX, minY + 2, DIAMOND, DIAMOND, DIAMOND)) {
            craftResult.id = CHESTPLATE_DIAMOND;
            craftResult.count = 1;
            return;
          }
          if (rw == 3
              && rh == 3
              && ck(g, w, minX, minY, DIAMOND, DIAMOND, DIAMOND)
              && ck(g, w, minX, minY + 1, DIAMOND, AIR, DIAMOND)
              && ck(g, w, minX, minY + 2, DIAMOND, AIR, DIAMOND)) {
            craftResult.id = LEGGINGS_DIAMOND;
            craftResult.count = 1;
            return;
          }
          if (rw == 2
              && rh == 2
              && ck2(g, w, minX, minY, DIAMOND, AIR)
              && ck2(g, w, minX, minY + 1, DIAMOND, DIAMOND)) {
            craftResult.id = BOOTS_DIAMOND;
            craftResult.count = 1;
            return;
          }
        }
      }
    }

    private boolean ck(byte[] g, int w, int x, int y, byte b1, byte b2, byte b3) {
      return ggi(g, w, x, y) == b1 && ggi(g, w, x + 1, y) == b2 && ggi(g, w, x + 2, y) == b3;
    }

    private boolean ck2(byte[] g, int w, int x, int y, byte b1, byte b2) {
      return ggi(g, w, x, y) == b1 && ggi(g, w, x + 1, y) == b2;
    }

    private byte ggi(byte[] g, int w, int x, int y) {
      if (x >= w || y >= (g.length / w)) return AIR;
      return g[x + y * w];
    }

    private void consumeCraft() {
      boolean wb = (gameState == 5);
      int w = wb ? 3 : 2, h = wb ? 3 : 2;
      for (int i = 0; i < w * h; i++) {
        if (craft[i].count > 0) {
          craft[i].count--;
          if (craft[i].count == 0) craft[i].id = 0;
        }
      }
    }

    private void initM3G() {
      g3d = Graphics3D.getInstance();
      camera = new Camera();
      camera.setPerspective(70.0f, (float) getWidth() / getHeight(), 0.1f, 100.0f);
      background = new Background();
      background.setColor(0x87CEEB);
      globalLight = new Light();
      globalLight.setMode(Light.AMBIENT);
      globalLight.setColor(0xFFFFFFFF);
      globalLight.setIntensity(1.0f);
      int[] p = new int[256];
      for (int i = 0; i < 256; i++) {
        int x = i % 16, y = i / 16;
        if (x == 0 || x == 15 || y == 0 || y == 15) p[i] = 0xFFFFFFFF;
        else p[i] = 0xFF999999;
      }
      Image ti = Image.createRGBImage(p, 16, 16, true);
      texBorder = new Texture2D(new Image2D(Image2D.RGB, ti));
      texBorder.setBlending(Texture2D.FUNC_MODULATE);
      texBorder.setFiltering(Texture2D.FILTER_NEAREST, Texture2D.FILTER_NEAREST);
      texBorder.setWrapping(Texture2D.WRAP_CLAMP, Texture2D.WRAP_CLAMP);
      int[] sp = new int[256];
      for (int i = 0; i < 256; i++) {
        int x = i % 16, y = i / 16, d = (x - 7) * (x - 7) + (y - 7) * (y - 7);
        if (d < 49) sp[i] = 0xAA000000;
        else sp[i] = 0;
      }
      Image si = Image.createRGBImage(sp, 16, 16, true);
      texShadow = new Texture2D(new Image2D(Image2D.RGBA, si));
      texShadow.setBlending(Texture2D.FUNC_MODULATE);
      matMain = new Material();
      matMain.setColor(Material.AMBIENT, 0xFFFFFFFF);
      matMain.setColor(Material.DIFFUSE, 0xFFFFFFFF);
      matMain.setColor(Material.EMISSIVE, 0);
      matMain.setVertexColorTrackingEnable(true);
      PolygonMode pm = new PolygonMode();
      pm.setCulling(PolygonMode.CULL_NONE);
      pm.setShading(PolygonMode.SHADE_FLAT);
      appWorld = new Appearance();
      appWorld.setMaterial(matMain);
      appWorld.setPolygonMode(pm);
      appWorld.setTexture(0, texBorder);
      appDrop = new Appearance();
      CompositingMode cmDrop = new CompositingMode();
      cmDrop.setAlphaThreshold(0.1f);
      cmDrop.setBlending(CompositingMode.ALPHA);
      appDrop.setCompositingMode(cmDrop);
      appDrop.setMaterial(matMain);
      appDrop.setPolygonMode(pm);
      appDrop.setTexture(0, texBorder);
      appSel = new Appearance();
      appSel.setPolygonMode(pm);
      CompositingMode cmS = new CompositingMode();
      cmS.setBlending(CompositingMode.ALPHA);
      cmS.setDepthOffset(-2.0f, -2.0f);
      appSel.setCompositingMode(cmS);
      int[] slP = new int[256];
      for (int i = 0; i < 256; i++) {
        int x = i % 16, y = i / 16;
        if (x == 0 || x == 15 || y == 0 || y == 15) slP[i] = 0xFF000000;
        else slP[i] = 0;
      }
      Texture2D tS =
          new Texture2D(new Image2D(Image2D.RGBA, Image.createRGBImage(slP, 16, 16, true)));
      tS.setBlending(Texture2D.FUNC_MODULATE);
      tS.setFiltering(Texture2D.FILTER_NEAREST, Texture2D.FILTER_NEAREST);
      appSel.setTexture(0, tS);
      short[] vS = new short[24 * 3], tSel = new short[24 * 2];
      addCubeToBuffer(vS, new byte[0], 0, 0, 0, 0, (short) V_SCALE, (byte) 0, (byte) 0, (byte) 0);
      for (int i = 0; i < 6; i++) {
        int o = i * 8;
        tSel[o] = 0;
        tSel[o + 1] = 0;
        tSel[o + 2] = 1;
        tSel[o + 3] = 0;
        tSel[o + 4] = 1;
        tSel[o + 5] = 1;
        tSel[o + 6] = 0;
        tSel[o + 7] = 1;
      }
      VertexArray vaP = new VertexArray(24, 3, 2);
      vaP.set(0, 24, vS);
      VertexArray vaT = new VertexArray(24, 2, 2);
      vaT.set(0, 24, tSel);
      VertexBuffer vbS = new VertexBuffer();
      vbS.setPositions(vaP, 1.02f / V_SCALE, null);
      vbS.setTexCoords(0, vaT, 1.0f, null);
      vbS.setDefaultColor(0xFFFFFFFF);
      int[] iS = {
        0, 1, 2, 0, 2, 3, 4, 5, 6, 4, 6, 7, 8, 9, 10, 8, 10, 11, 12, 13, 14, 12, 14, 15, 16, 17, 18,
        16, 18, 19, 20, 21, 22, 20, 22, 23
      };
      int[] lS = new int[12];
      for (int i = 0; i < 12; i++) lS[i] = 3;
      selMesh = new Mesh(vbS, new TriangleStripArray(iS, lS), appSel);
      VertexBuffer vbItem = new VertexBuffer();
      vbItem.setPositions(vaP, 1.0f / V_SCALE, null);
      vbItem.setTexCoords(0, vaT, 1.0f, null);
      vbItem.setDefaultColor(0xFFFFFFFF);
      itemMesh = new Mesh(vbItem, new TriangleStripArray(iS, lS), appDrop);
      appCracks = new Appearance[10];
      CompositingMode cmC = new CompositingMode();
      cmC.setBlending(CompositingMode.ALPHA);
      cmC.setDepthWriteEnable(false);
      cmC.setDepthOffset(-1.0f, -1.0f);
      for (int i = 0; i < 10; i++) {
        appCracks[i] = new Appearance();
        appCracks[i].setPolygonMode(pm);
        appCracks[i].setCompositingMode(cmC);
        appCracks[i].setTexture(0, gct(i));
      }
      VertexBuffer vbC = new VertexBuffer();
      vbC.setPositions(vaP, 1.01f / V_SCALE, null);
      vbC.setTexCoords(0, vaT, 1.0f, null);
      vbC.setDefaultColor(0xFFFFFFFF);
      crackMesh = new Mesh(vbC, new TriangleStripArray(iS, lS), appCracks[0]);
      appHand = new Appearance();
      appHand.setMaterial(matMain);
      appHand.setPolygonMode(pm);
      appHand.setTexture(0, texBorder);
      short[] vD = {
        -4, -4, 4, 4, -4, 4, 4, 4, 4, -4, 4, 4, -4, -4, -4, 4, -4, -4, 4, 4, -4, -4, 4, -4
      };
      VertexArray vaD = new VertexArray(8, 3, 2);
      vaD.set(0, 8, vD);
      dropVB = new VertexBuffer();
      dropVB.setPositions(vaD, 0.1f, null);
      dropVB.setDefaultColor(0xFFFFFFFF);
      int[] iD = {
        0, 1, 2, 0, 2, 3, 1, 5, 6, 1, 6, 2, 5, 4, 7, 5, 7, 6, 4, 0, 3, 4, 3, 7, 3, 2, 6, 3, 6, 7, 4,
        5, 1, 4, 1, 0
      };
      int[] lD = new int[12];
      for (int i = 0; i < 12; i++) lD[i] = 3;
      dropIB = new TriangleStripArray(iD, lD);
      short[] vF = {-4, -4, 0, 4, -4, 0, 4, 4, 0, -4, 4, 0};
      VertexArray vaF = new VertexArray(4, 3, 2);
      vaF.set(0, 4, vF);
      short[] tF = {0, 1, 1, 1, 1, 0, 0, 0};
      VertexArray vaFT = new VertexArray(4, 2, 2);
      vaFT.set(0, 4, tF);
      dropFlatVB = new VertexBuffer();
      dropFlatVB.setPositions(vaF, 0.1f, null);
      dropFlatVB.setTexCoords(0, vaFT, 1.0f, null);
      dropFlatVB.setDefaultColor(0xFFFFFFFF);
      int[] iF = {0, 1, 2, 0, 2, 3}, lF = {3, 3};
      dropFlatIB = new TriangleStripArray(iF, lF);
      short[] vSh = {-5, 0, -5, 5, 0, -5, 5, 0, 5, -5, 0, 5};
      VertexArray vaSh = new VertexArray(4, 3, 2);
      vaSh.set(0, 4, vSh);
      shadowVB = new VertexBuffer();
      shadowVB.setPositions(vaSh, 0.0625f, null);
      shadowVB.setTexCoords(0, vaFT, 1.0f, null);
      shadowVB.setDefaultColor(0xFFFFFFFF);
      shadowIB = new TriangleStripArray(iF, lF);
      appShadow = new Appearance();
      Material ms = new Material();
      ms.setColor(Material.EMISSIVE, 0xFFFFFFFF);
      appShadow.setMaterial(ms);
      appShadow.setTexture(0, texShadow);
      appShadow.setPolygonMode(pm);
      CompositingMode cm = new CompositingMode();
      cm.setBlending(CompositingMode.ALPHA);
      appShadow.setCompositingMode(cm);
      handMesh = new Mesh(dropVB, dropIB, appHand);
      createCrossedMesh();
      short[] uvFix = {
        0, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0,
        0, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1
      };
      VertexArray vaFix = new VertexArray(24, 2, 2);
      vaFix.set(0, 24, uvFix);
      vbItem.setTexCoords(0, vaFix, 1.0f, null);
      appItemTop = new Appearance();
      appItemTop.setMaterial(matMain);
      appItemTop.setPolygonMode(pm);
      appItemTop.setCompositingMode(cmDrop);
      appItemBot = new Appearance();
      appItemBot.setMaterial(matMain);
      appItemBot.setPolygonMode(pm);
      appItemBot.setCompositingMode(cmDrop);
      appItemFront = new Appearance();
      appItemFront.setMaterial(matMain);
      appItemFront.setPolygonMode(pm);
      appItemFront.setCompositingMode(cmDrop);
      appItemBack = new Appearance();
      appItemBack.setMaterial(matMain);
      appItemBack.setPolygonMode(pm);
      appItemBack.setCompositingMode(cmDrop);
      appItemLeft = new Appearance();
      appItemLeft.setMaterial(matMain);
      appItemLeft.setPolygonMode(pm);
      appItemLeft.setCompositingMode(cmDrop);
      appItemRight = new Appearance();
      appItemRight.setMaterial(matMain);
      appItemRight.setPolygonMode(pm);
      appItemRight.setCompositingMode(cmDrop);
      int[] iIf = {0, 1, 2, 0, 2, 3};
      int[] iIbk = {4, 5, 6, 4, 6, 7};
      int[] iIl = {8, 9, 10, 8, 10, 11};
      int[] iIr = {12, 13, 14, 12, 14, 15};
      int[] iIt = {16, 17, 18, 16, 18, 19};
      int[] iIb = {20, 21, 22, 20, 22, 23};
      int[] l3 = {3, 3};
      IndexBuffer[] ibs = {
        new TriangleStripArray(iIf, l3),
        new TriangleStripArray(iIbk, l3),
        new TriangleStripArray(iIl, l3),
        new TriangleStripArray(iIr, l3),
        new TriangleStripArray(iIt, l3),
        new TriangleStripArray(iIb, l3)
      };
      Appearance[] aps = {
        appItemFront, appItemBack, appItemLeft, appItemRight, appItemTop, appItemBot
      };
      itemMesh = new Mesh(vbItem, ibs, aps);
    }

    private void createCrossedMesh() {
      short[] v = {
        -4, -4, -4, 4, -4, 4, 4, 4, 4, -4, 4, -4, -4, -4, 4, 4, -4, -4, 4, 4, -4, -4, 4, 4
      };
      short[] t = {0, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0};
      int[] ind = {0, 1, 2, 0, 2, 3, 4, 5, 6, 4, 6, 7};
      int[] len = {3, 3, 3, 3};
      VertexArray va = new VertexArray(8, 3, 2);
      va.set(0, 8, v);
      VertexArray ta = new VertexArray(8, 2, 2);
      ta.set(0, 8, t);
      VertexBuffer vb = new VertexBuffer();
      vb.setPositions(va, 0.1f, null);
      vb.setTexCoords(0, ta, 1.0f, null);
      vb.setDefaultColor(0xFFFFFFFF);
      crossedMesh = new Mesh(vb, new TriangleStripArray(ind, len), appDrop);
    }

    private Texture2D gct(int s) {
      int[] p = new int[256];
      Random r = new Random(s * 12345);
      for (int i = 0; i < 256; i++) p[i] = 0;
      if (s > 0) {
        int cx = 8, cy = 8;
        for (int b = 0; b < 3 + s / 2; b++) {
          float x = cx, y = cy, a = (float) (r.nextDouble() * 6.28);
          int l = 3 + s;
          for (int k = 0; k < l; k++) {
            int px = (int) x, py = (int) y;
            if (px >= 0 && px < 16 && py >= 0 && py < 16) p[px + py * 16] = 0xFF000000;
            x += Math.cos(a);
            y += Math.sin(a);
            a += (r.nextDouble() - 0.5);
          }
        }
      }
      Texture2D t = new Texture2D(new Image2D(Image2D.RGBA, Image.createRGBImage(p, 16, 16, true)));
      t.setBlending(Texture2D.FUNC_MODULATE);
      t.setFiltering(Texture2D.FILTER_NEAREST, Texture2D.FILTER_NEAREST);
      return t;
    }

    private void generateCloudMesh() {
      boolean[] m = new boolean[CLOUD_RES * CLOUD_RES];
      Random r = new Random();
      for (int i = 0; i < m.length; i++) m[i] = r.nextInt(100) < 55;
      for (int k = 0; k < 2; k++) {
        boolean[] n = new boolean[CLOUD_RES * CLOUD_RES];
        for (int x = 0; x < CLOUD_RES; x++)
          for (int z = 0; z < CLOUD_RES; z++) {
            int c = 0;
            for (int dx = -1; dx <= 1; dx++)
              for (int dz = -1; dz <= 1; dz++) {
                int nx = x + dx, nz = z + dz;
                if (nx >= 0
                    && nx < CLOUD_RES
                    && nz >= 0
                    && nz < CLOUD_RES
                    && m[nx + nz * CLOUD_RES]) c++;
              }
            n[x + z * CLOUD_RES] = (c > 4);
          }
        m = n;
      }
      int c = 0;
      for (int i = 0; i < m.length; i++) if (m[i]) c++;
      if (c == 0) return;
      VertexArray vaP = new VertexArray(c * 24, 3, 2), vaC = new VertexArray(c * 24, 3, 1);
      short[] v = new short[c * 24 * 3];
      byte[] cl = new byte[c * 24 * 3];
      int idx = 0;
      for (int x = 0; x < CLOUD_RES; x++)
        for (int z = 0; z < CLOUD_RES; z++) {
          if (m[x + z * CLOUD_RES]) {
            addCubeToBuffer(
                v,
                cl,
                idx * 24 * 3,
                x * CLOUD_SCALE,
                CLOUD_H,
                z * CLOUD_SCALE,
                (short) (CLOUD_SCALE * V_SCALE),
                (byte) 255,
                (byte) 255,
                (byte) 255);
            idx++;
          }
        }
      vaP.set(0, c * 24, v);
      vaC.set(0, c * 24, cl);
      VertexBuffer cvb = new VertexBuffer();
      cvb.setPositions(vaP, 1.0f / V_SCALE, null);
      cvb.setColors(vaC);
      cvb.setDefaultColor(0xFFFFFFFF);
      int[] ind = new int[c * 12 * 3], len = new int[c * 12];
      for (int i = 0; i < c; i++) {
        int vo = i * 24, io = i * 36;
        int[] li = {
          0, 1, 2, 0, 2, 3, 4, 5, 6, 4, 6, 7, 8, 9, 10, 8, 10, 11, 12, 13, 14, 12, 14, 15, 16, 17,
          18, 16, 18, 19, 20, 21, 22, 20, 22, 23
        };
        for (int j = 0; j < 36; j++) ind[io + j] = vo + li[j];
        for (int j = 0; j < 12; j++) len[i * 12 + j] = 3;
      }
      IndexBuffer cib = new TriangleStripArray(ind, len);
      appClouds = new Appearance();
      cloudMat = new Material();
      cloudMat.setColor(Material.EMISSIVE, 0xFFFFFFFF);
      cloudMat.setVertexColorTrackingEnable(true);
      PolygonMode pm = new PolygonMode();
      pm.setCulling(PolygonMode.CULL_NONE);
      appClouds.setMaterial(cloudMat);
      appClouds.setPolygonMode(pm);
      cloudMesh3D = new Mesh(cvb, cib, appClouds);
      VertexArray va2D = new VertexArray(c * 4, 3, 2);
      short[] v2 = new short[c * 4 * 3];
      idx = 0;
      for (int x = 0; x < CLOUD_RES; x++)
        for (int z = 0; z < CLOUD_RES; z++) {
          if (m[x + z * CLOUD_RES]) {
            int X = x * CLOUD_SCALE * V_SCALE,
                Y = CLOUD_H * V_SCALE,
                Z = z * CLOUD_SCALE * V_SCALE,
                S = CLOUD_SCALE * V_SCALE;
            int o = idx * 12;
            v2[o] = (short) (X + S);
            v2[o + 1] = (short) Y;
            v2[o + 2] = (short) Z;
            v2[o + 3] = (short) X;
            v2[o + 4] = (short) Y;
            v2[o + 5] = (short) Z;
            v2[o + 6] = (short) X;
            v2[o + 7] = (short) Y;
            v2[o + 8] = (short) (Z + S);
            v2[o + 9] = (short) (X + S);
            v2[o + 10] = (short) Y;
            v2[o + 11] = (short) (Z + S);
            idx++;
          }
        }
      va2D.set(0, c * 4, v2);
      int[] i2 = new int[c * 6], l2 = new int[c * 2];
      for (int i = 0; i < c; i++) {
        int vo = i * 4;
        i2[i * 6] = vo;
        i2[i * 6 + 1] = vo + 2;
        i2[i * 6 + 2] = vo + 3;
        i2[i * 6 + 3] = vo;
        i2[i * 6 + 4] = vo + 1;
        i2[i * 6 + 5] = vo + 2;
        l2[i * 2] = 3;
        l2[i * 2 + 1] = 3;
      }
      VertexBuffer vb2 = new VertexBuffer();
      vb2.setPositions(va2D, 1.0f / V_SCALE, null);
      vb2.setDefaultColor(0xFFFFFFFF);
      cloudMesh2D = new Mesh(vb2, new TriangleStripArray(i2, l2), appClouds);
    }

    private void addCubeToBuffer(
        short[] v, byte[] c, int o, int x, int y, int z, short s, byte r, byte g, byte b) {
      short X = (short) (x * V_SCALE),
          Y = (short) (y * V_SCALE),
          Z = (short) (z * V_SCALE),
          x1 = (short) (X + s),
          y1 = (short) (Y + s),
          z1 = (short) (Z + s);
      v[o++] = X;
      v[o++] = Y;
      v[o++] = z1;
      v[o++] = x1;
      v[o++] = Y;
      v[o++] = z1;
      v[o++] = x1;
      v[o++] = y1;
      v[o++] = z1;
      v[o++] = X;
      v[o++] = y1;
      v[o++] = z1;
      v[o++] = x1;
      v[o++] = Y;
      v[o++] = Z;
      v[o++] = X;
      v[o++] = Y;
      v[o++] = Z;
      v[o++] = X;
      v[o++] = y1;
      v[o++] = Z;
      v[o++] = x1;
      v[o++] = y1;
      v[o++] = Z;
      v[o++] = X;
      v[o++] = Y;
      v[o++] = Z;
      v[o++] = X;
      v[o++] = Y;
      v[o++] = z1;
      v[o++] = X;
      v[o++] = y1;
      v[o++] = z1;
      v[o++] = X;
      v[o++] = y1;
      v[o++] = Z;
      v[o++] = x1;
      v[o++] = Y;
      v[o++] = z1;
      v[o++] = x1;
      v[o++] = Y;
      v[o++] = Z;
      v[o++] = x1;
      v[o++] = y1;
      v[o++] = Z;
      v[o++] = x1;
      v[o++] = y1;
      v[o++] = z1;
      v[o++] = X;
      v[o++] = y1;
      v[o++] = z1;
      v[o++] = x1;
      v[o++] = y1;
      v[o++] = z1;
      v[o++] = x1;
      v[o++] = y1;
      v[o++] = Z;
      v[o++] = X;
      v[o++] = y1;
      v[o++] = Z;
      v[o++] = X;
      v[o++] = Y;
      v[o++] = Z;
      v[o++] = x1;
      v[o++] = Y;
      v[o++] = Z;
      v[o++] = x1;
      v[o++] = Y;
      v[o++] = z1;
      v[o++] = X;
      v[o++] = Y;
      v[o++] = z1;
      if (c.length > 0) {
        o -= 72;
        for (int i = 0; i < 72; i += 3) {
          c[o + i] = r;
          c[o + i + 1] = g;
          c[o + i + 2] = b;
        }
      }
    }

    private void generateWorld() {
      if (loadingTips.length > 0) {
        loadingTip = loadingTips[Math.abs(rand.nextInt()) % loadingTips.length];
      }
      drawLoading(0.0f);
      if (currentDim == -1) {
        generateNether();
        return;
      }
      world = new byte[WORLD_X * WORLD_Y * WORLD_H];
      worldData = new byte[WORLD_X * WORLD_Y * WORLD_H];
      drops.removeAllElements();
      fallingBlocks.removeAllElements();
      for (int i = 0; i < 9; i++) {
        hotbar[i].id = 0;
        hotbar[i].count = 0;
      }
      for (int i = 0; i < 27; i++) {
        inventory[i].id = 0;
        inventory[i].count = 0;
      }
      health = 20;
      food = 20;
      air = 300;
      if (setupType == 1) {
        for (int x = 0; x < WORLD_X; x++) {
          drawLoading((float) x / (float) WORLD_X);
          for (int z = 0; z < WORLD_Y; z++) {
            setBlock(x, 0, z, BEDROCK);
            setBlock(x, 1, z, DIRT);
            setBlock(x, 2, z, DIRT);
            setBlock(x, 3, z, GRASS);
            for (int y = 4; y < WORLD_H; y++) setBlock(x, y, z, AIR);
            if (x == 0 || x == WORLD_X - 1 || z == 0 || z == WORLD_Y - 1) {
              for (int y = 0; y < WORLD_H; y++) {
                setBlock(x, y, z, BARRIER);
                setData(x, y, z, 1);
              }
            }
          }
        }
        px = 128.5f;
        py = 5.5f;
        pz = 128.5f;
        for (int i = 0; i < chunks.length; i++) chunks[i].dirty = true;
        new VillageGenerator().generate();
        generateCloudMesh();
        return;
      }
      Random r = new Random(getSeedLong());
      rand.setSeed(getSeedLong());
      int[] hm = new int[WORLD_X * WORLD_Y];
      byte[] bio = new byte[WORLD_X * WORLD_Y];
      int gs = 16;
      float[][] cp = new float[CHUNKS_X + 1][CHUNKS_Z + 1];
      float[][] tp = new float[CHUNKS_X + 1][CHUNKS_Z + 1];
      float[][] rp = new float[CHUNKS_X + 1][CHUNKS_Z + 1];
      for (int x = 0; x <= CHUNKS_X; x++)
        for (int z = 0; z <= CHUNKS_Z; z++) {
          cp[x][z] = r.nextFloat();
          tp[x][z] = r.nextFloat();
          rp[x][z] = r.nextFloat();
        }
      for (int x = 0; x < WORLD_X; x++)
        for (int z = 0; z < WORLD_Y; z++) {
          int gx = x / 16, gz = z / 16;
          float tx = (x % 16) / 16.0f, tz = (z % 16) / 16.0f;
          float top = cp[gx][gz] * (1 - tx) + cp[gx + 1][gz] * tx,
              bot = cp[gx][gz + 1] * (1 - tx) + cp[gx + 1][gz + 1] * tx,
              val = top * (1 - tz) + bot * tz;
          float tVal =
              (tp[gx][gz] * (1 - tx) + tp[gx + 1][gz] * tx) * (1 - tz)
                  + (tp[gx][gz + 1] * (1 - tx) + tp[gx + 1][gz + 1] * tx) * tz;
          float rVal =
              (rp[gx][gz] * (1 - tx) + rp[gx + 1][gz] * tx) * (1 - tz)
                  + (rp[gx][gz + 1] * (1 - tx) + rp[gx + 1][gz + 1] * tx) * tz;
          hm[x + z * WORLD_X] = 8 + (int) (val * 34);
          int bi = 2;
          if (tVal < 0.35) {
            bi = (rVal > 0.5) ? 8 : 5;
          } else if (tVal > 0.65) {
            bi = (rVal < 0.4) ? 6 : ((rVal < 0.6) ? 4 : 0);
          } else {
            bi = (rVal < 0.3) ? 7 : ((rVal > 0.6) ? 3 : ((rVal > 0.45) ? 1 : 2));
          }
          bio[x + z * WORLD_X] = (byte) bi;
        }
      for (int x = 0; x < WORLD_X; x++) {
        drawLoading((float) x / (float) WORLD_X);
        for (int z = 0; z < WORLD_Y; z++) {
          int h = hm[x + z * WORLD_X], bi = bio[x + z * WORLD_X] & 0xFF;
          int dd = 3 + r.nextInt(2);
          boolean cliff = (h > 46 && ((r.nextInt() & 1) != 0));
          if (cliff) dd = 0;
          if (bi == 3 && h > SEA_LEVEL) h = SEA_LEVEL + 1;
          setBlock(x, 0, z, BEDROCK);
          byte topB = GRASS, fillB = DIRT;
          boolean beach =
              (h >= SEA_LEVEL && h <= SEA_LEVEL + 2 && bi != 3 && bi != 8 && bi != 0 && !cliff);
          if (bi == 6) {
            topB = SAND;
            fillB = SAND;
          } else if (bi == 8) {
            topB = SNOW_BLOCK;
          } else if (beach) {
            topB = SAND;
            fillB = SAND;
          }
          if (h < SEA_LEVEL && topB == GRASS) topB = DIRT;
          if (h < SEA_LEVEL && topB == SNOW_BLOCK) topB = DIRT;
          for (int y = 1; y < WORLD_H; y++) {
            if (y < h - dd) {
              byte b = STONE;
              if (y < 12 && r.nextInt(100) < 5) b = LAVA;
              else if (r.nextInt(100) < 2) b = ORE_COAL;
              else if (y < 20 && r.nextInt(100) < 1) b = ORE_IRON;
              else if (y < 12 && r.nextInt(200) < 1) b = ORE_GOLD;
              else if (y < 10 && r.nextInt(300) < 1) b = ORE_DIAMOND;
              else if (r.nextInt(100) < 3) b = GRAVEL;
              else if (y < 16 && r.nextInt(100) < 2) b = ORE_REDSTONE;
              setBlock(x, y, z, b);
            } else if (y < h) {
              setBlock(x, y, z, fillB);
            } else if (y == h) {
              setBlock(x, y, z, topB);
              if (y > SEA_LEVEL && topB == GRASS && !cliff && !beach) {
                int v = r.nextInt(100);
                if (v < 10) setBlock(x, y + 1, z, SHORT_GRASS);
                else if (v < 15) setBlock(x, y + 1, z, PLANT_TALL_GRASS);
                else if (v < 17) setBlock(x, y + 1, z, FLOWER_YELLOW);
                else if (v < 19) setBlock(x, y + 1, z, FLOWER_RED);
                else if (v < 20) setBlock(x, y + 1, z, MUSHROOM_BROWN);
                else if (v < 21) setBlock(x, y + 1, z, MUSHROOM_RED);
                if (r.nextInt(400) == 0) {
                  setBlock(x, y + 1, z, PUMPKIN);
                  if (x > 0
                      && getBlock(x - 1, y, z) != AIR
                      && getBlock(x - 1, y + 1, z) == AIR
                      && ((r.nextInt() & 1) != 0)) setBlock(x - 1, y + 1, z, PUMPKIN);
                  if (z > 0
                      && getBlock(x, y, z - 1) != AIR
                      && getBlock(x, y + 1, z - 1) == AIR
                      && ((r.nextInt() & 1) != 0)) setBlock(x, y + 1, z - 1, PUMPKIN);
                  if (x < WORLD_X - 1
                      && getBlock(x + 1, y, z) != AIR
                      && getBlock(x + 1, y + 1, z) == AIR
                      && ((r.nextInt() & 1) != 0)) setBlock(x + 1, y + 1, z, PUMPKIN);
                  if (z < WORLD_Y - 1
                      && getBlock(x, y, z + 1) != AIR
                      && getBlock(x, y + 1, z + 1) == AIR
                      && ((r.nextInt() & 1) != 0)) setBlock(x, y + 1, z + 1, PUMPKIN);
                }
              }
              if (bi == 6 && topB == SAND && !cliff && !beach && y > SEA_LEVEL) {
                if (r.nextInt(50) == 0) setBlock(x, y + 1, z, DEAD_BUSH);
              }
              if ((topB == SAND || topB == DIRT || topB == GRASS)
                  && !cliff
                  && (h == SEA_LEVEL || h == SEA_LEVEL + 1)) {
                int chance = (bi == 6) ? 20 : 60;
                if (r.nextInt(chance) == 0) {
                  int rh = 2 + r.nextInt(2);
                  for (int k = 1; k <= rh; k++) setBlock(x, y + k, z, REEDS);
                }
              }
              if (y > SEA_LEVEL
                  && !cliff
                  && !beach
                  && x > 2
                  && x < WORLD_X - 2
                  && z > 2
                  && z < WORLD_Y - 2) {
                int tc = 0;
                byte log = WOOD, lvs = LEAVES;
                boolean isSpruce = false;
                if (bi == 0) {
                  tc = 12;
                  log = WOOD_JUNGLE;
                  lvs = LEAVES_JUNGLE;
                }
                if (bi == 1) {
                  tc = 2;
                  if (((r.nextInt() & 1) != 0)) {
                    log = WOOD_BIRCH;
                    lvs = LEAVES_BIRCH;
                  }
                }
                if (bi == 2) {
                  tc = 5;
                  if (r.nextInt(3) == 0) {
                    log = WOOD_BIRCH;
                    lvs = LEAVES_BIRCH;
                  }
                }
                if (bi == 3) {
                  tc = 1;
                }
                if (bi == 4) {
                  tc = 1;
                  log = WOOD_ACACIA;
                  lvs = LEAVES_ACACIA;
                }
                if (bi == 5) {
                  tc = 8;
                  log = WOOD_SPRUCE;
                  lvs = LEAVES_SPRUCE;
                  isSpruce = true;
                }
                if (bi == 6 && r.nextInt(100) < 2) {
                  int ch = 1 + r.nextInt(3);
                  for (int c = 0; c < ch; c++) setBlock(x, y + 1 + c, z, CACTUS);
                }
                if (bi == 8) {
                  tc = 1;
                  log = WOOD_SPRUCE;
                  lvs = LEAVES_SPRUCE;
                  isSpruce = true;
                }
                if (tc > 0 && r.nextInt(100) < tc) {
                  if (isSpruce) {
                    int th = 6 + r.nextInt(4);
                    for (int i = 1; i <= th; i++) setBlock(x, y + i, z, log);
                    for (int ly = y + 3; ly <= y + th; ly++) {
                      int rad = (ly > y + th - 3) ? 1 : 2;
                      for (int lx = x - rad; lx <= x + rad; lx++)
                        for (int lz = z - rad; lz <= z + rad; lz++) {
                          if (Math.abs(lx - x) + Math.abs(lz - z) <= rad + 1)
                            setBlock(lx, ly, lz, lvs);
                        }
                    }
                    setBlock(x, y + th + 1, z, lvs);
                  } else {
                    int th = 4 + r.nextInt(2);
                    for (int i = 1; i <= th; i++) setBlock(x, y + i, z, log);
                    for (int lx = x - 2; lx <= x + 2; lx++)
                      for (int lz = z - 2; lz <= z + 2; lz++) {
                        boolean crn = (Math.abs(lx - x) == 2 && Math.abs(lz - z) == 2);
                        if (!crn || ((r.nextInt() & 1) != 0)) {
                          if (lx != x || lz != z) setBlock(lx, y + th - 1, lz, lvs);
                          if ((lx != x || lz != z) && Math.abs(lx - x) < 2 && Math.abs(lz - z) < 2)
                            setBlock(lx, y + th, lz, lvs);
                        }
                      }
                    setBlock(x, y + th + 1, z, lvs);
                  }
                }
              }
            } else if (y > h) {
              if (y < SEA_LEVEL) {
                setBlock(x, y, z, WATER);
              } else if (y == SEA_LEVEL) {
                if (bi == 8) setBlock(x, y, z, ICE);
                else setBlock(x, y, z, WATER);
              }
            }
          }
        }
      }
      for (int y = 0; y < WORLD_H; y++) {
        for (int x = 0; x < WORLD_X; x++) {
          setBlock(x, y, 0, BARRIER);
          setData(x, y, 0, 1);
          setBlock(x, y, WORLD_Y - 1, BARRIER);
          setData(x, y, WORLD_Y - 1, 1);
        }
        for (int z = 0; z < WORLD_Y; z++) {
          setBlock(0, y, z, BARRIER);
          setData(0, y, z, 1);
          setBlock(WORLD_X - 1, y, z, BARRIER);
          setData(WORLD_X - 1, y, z, 1);
        }
      }
      px = 128.5f;
      pz = 128.5f;
      for (int y = WORLD_H - 1; y >= 0; y--) {
        byte b = getBlock((int) px, y, (int) pz);
        if (b != AIR && b != BARRIER) {
          if (isWater(b) || isLava(b)) py = SEA_LEVEL + 1.5f;
          else py = y + 2.5f;
          break;
        }
      }
      for (int i = 0; i < chunks.length; i++) chunks[i].dirty = true;
      new VillageGenerator().generate();
      generateCloudMesh();
      drawLoading(1.0f);
    }

    private void initPortalAnim() {
      try {
        Image i = Image.createImage("/j2me_textures/animations/portal_animation.png");
        pFrms = new Image[32];
        pImg2Ds = new Image2D[32];
        for (int k = 0; k < 32; k++) {
          pFrms[k] = Image.createImage(i, 0, k * 16, 16, 16, 0);
          pImg2Ds[k] = new Image2D(Image2D.RGBA, pFrms[k]);
        }
        pTex = new Texture2D(pImg2Ds[0]);
        pTex.setFiltering(Texture2D.FILTER_NEAREST, Texture2D.FILTER_NEAREST);
        pTex.setWrapping(Texture2D.WRAP_CLAMP, Texture2D.WRAP_CLAMP);
        pTex.setBlending(Texture2D.FUNC_MODULATE);
      } catch (Exception e) {
      }
    }

    private void updatePortalAnim() {
      if (setAnimations == 0 || pImg2Ds == null) return;
      pTick++;
      if (pTex != null) {
        try {
          pTex.setImage(pImg2Ds[pTick % 32]);
        } catch (Exception e) {
        }
      }
    }

    private void setBlock(int x, int y, int z, byte id) {
      if (x >= 0 && x < WORLD_X && z >= 0 && z < WORLD_Y && y >= 0 && y < WORLD_H)
        world[x + z * WORLD_X + y * (WORLD_X * WORLD_Y)] = id;
    }

    private void setData(int x, int y, int z, int d) {
      if (x >= 0 && x < WORLD_X && z >= 0 && z < WORLD_Y && y >= 0 && y < WORLD_H)
        worldData[x + z * WORLD_X + y * (WORLD_X * WORLD_Y)] = (byte) d;
    }

    private byte getData(int x, int y, int z) {
      if (x >= 0 && x < WORLD_X && z >= 0 && z < WORLD_Y && y >= 0 && y < WORLD_H)
        return worldData[x + z * WORLD_X + y * (WORLD_X * WORLD_Y)];
      return 0;
    }

    private void setBlockAndDirty(int x, int y, int z, byte id) {
      if (id == AIR) {
        byte b0 = getBlock(x, y, z);
        if (b0 == REEDS || b0 == CACTUS) {
          int cy = y + 1;
          while (cy < WORLD_H) {
            byte abv = getBlock(x, cy, z);
            if (abv == b0) {
              setBlock(x, cy, z, AIR);
              markChunkDirtyAt(x, z);
              drops.addElement(new Drop(x + 0.5f, cy + 0.5f, z + 0.5f, b0, 0, 0, 0, 1, 500));
              cy++;
            } else {
              break;
            }
          }
        }
        if (b0 == PLANT_TALL_GRASS) {
          int d = getData(x, y, z);
          if (d == 1) {
            if (getBlock(x, y - 1, z) == PLANT_TALL_GRASS) {
              setBlock(x, y - 1, z, AIR);
              markChunkDirtyAt(x, z);
            }
          } else {
            if (getBlock(x, y + 1, z) == PLANT_TALL_GRASS) {
              setBlock(x, y + 1, z, AIR);
              markChunkDirtyAt(x, z);
            }
          }
        }
        if (b0 == BED_BLOCK) {
          int d = getData(x, y, z);
          int dir = d & 3;
          boolean isHead = (d & 8) != 0;
          int dx = 0, dz = 0;
          if (dir == 0) dz = -1;
          else if (dir == 1) dx = -1;
          else if (dir == 2) dz = 1;
          else if (dir == 3) dx = 1;
          int ox = x + (isHead ? -dx : dx);
          int oz = z + (isHead ? -dz : dz);
          if (getBlock(ox, y, oz) == BED_BLOCK) {
            setBlock(ox, y, oz, AIR);
            markChunkDirtyAt(ox, oz);
          }
        }
        if (b0 == WOOD_DOOR) {
          int d = getData(x, y, z);
          int dy = ((d & 8) != 0) ? -1 : 1;
          if (getBlock(x, y + dy, z) == WOOD_DOOR) {
            setBlock(x, y + dy, z, AIR);
            markChunkDirtyAt(x, z);
          }
        }
      }
      byte old = getBlock(x, y, z);
      if (old != id && old != AIR) {
        removeTileEntity(x, y, z);
      }
      if (id != old && (id == CHEST || id == FURNACE)) {
        createTileEntity(x, y, z, id);
      }
      if (!ignoreBreakCheck && old != id) checkPortalBreak(x, y, z, old);
      setBlock(x, y, z, id);
      checkGravity(x, y, z);
      checkGravity(x, y + 1, z);
      setData(x, y, z, 0);
      updateLightAt(x, y, z);
      int cx = x / CHUNK_SIZE, cz = z / CHUNK_SIZE;
      if (cx >= 0 && cx < CHUNKS_X && cz >= 0 && cz < CHUNKS_Z) {
        chunks[cx + cz * CHUNKS_X].dirty = true;
        if (x % CHUNK_SIZE == 0 && cx > 0) chunks[(cx - 1) + cz * CHUNKS_X].dirty = true;
        if (x % CHUNK_SIZE == 15 && cx < CHUNKS_X - 1)
          chunks[(cx + 1) + cz * CHUNKS_X].dirty = true;
        if (z % CHUNK_SIZE == 0 && cz > 0) chunks[cx + (cz - 1) * CHUNKS_X].dirty = true;
        if (z % CHUNK_SIZE == 15 && cz < CHUNKS_Z - 1)
          chunks[cx + (cz + 1) * CHUNKS_X].dirty = true;
      }
    }

    private byte getBlock(int x, int y, int z) {
      if (x >= 0 && x < WORLD_X && z >= 0 && z < WORLD_Y && y >= 0 && y < WORLD_H)
        return world[x + z * WORLD_X + y * (WORLD_X * WORLD_Y)];
      return AIR;
    }

    private int addToInventory(byte id, int c) {
      if (id == 0) return c;
      if ((id >= WOOD_PICKAXE && id <= WOOD_SWORD)
          || id == STONE_PICKAXE
          || id == STONE_AXE
          || id == STONE_SHOVEL
          || id == STONE_SWORD
          || id == STONE_HOE
          || (id >= WOOD_HOE && id <= DIAMOND_HOE)
          || isArmorItem(id)
          || id == FLINT_AND_STEEL
          || id == BUCKET
          || id == BUCKET_WATER
          || id == BUCKET_LAVA) {
        for (int i = 0; i < 9; i++)
          if (hotbar[i].count == 0) {
            hotbar[i].id = id;
            hotbar[i].count = 1;
            return c - 1;
          }
        for (int i = 0; i < 27; i++)
          if (inventory[i].count == 0) {
            inventory[i].id = id;
            inventory[i].count = 1;
            return c - 1;
          }
        return c;
      }
      for (int i = 0; i < 9; i++)
        if (hotbar[i].id == id && hotbar[i].count < 64) {
          int space = 64 - hotbar[i].count;
          int add = Math.min(c, space);
          hotbar[i].count += add;
          c -= add;
          if (c <= 0) return 0;
        }
      for (int i = 0; i < 27; i++)
        if (inventory[i].id == id && inventory[i].count < 64) {
          int space = 64 - inventory[i].count;
          int add = Math.min(c, space);
          inventory[i].count += add;
          c -= add;
          if (c <= 0) return 0;
        }
      for (int i = 0; i < 9; i++)
        if (hotbar[i].count == 0) {
          hotbar[i].id = id;
          hotbar[i].count = c;
          return 0;
        }
      for (int i = 0; i < 27; i++)
        if (inventory[i].count == 0) {
          inventory[i].id = id;
          inventory[i].count = c;
          return 0;
        }
      return c;
    }

    private void throwItem() {
      if (hotbar[selectedSlot].count > 0) {
        byte id = hotbar[selectedSlot].id;
        hotbar[selectedSlot].count--;
        if (hotbar[selectedSlot].count <= 0) hotbar[selectedSlot].id = 0;
        Transform t = new Transform();
        t.postRotate(ry, 0, 1, 0);
        t.postRotate(rx, 1, 0, 0);
        float[] v = {0, 0, -1, 1};
        t.transform(v);
        float tp = 0.4f;
        drops.addElement(
            new Drop(px, py + 1.5f, pz, id, v[0] * tp, v[1] * tp + 0.1f, v[2] * tp, 1, 2000));
      }
    }

    private float getBlockHardness(byte id, byte t) {
      if (id == GRASS_PATH) return 0.6f;
      if (id == IRON_BARS) return 5.0f;
      if (id == GLASS_PANE) return 0.3f;
      if (creativeMode) return 100.0f;
      float b = 1.0f;
      switch (id) {
        case STAIRS_WOOD:
        case STAIRS_COBBLE:
        case FENCE:
        case BOOKSHELF:
          return 2.0f;
        case LEAVES:
          b = 0.2f;
          break;
        case GRASS:
        case DIRT:
        case SAND:
        case GRAVEL:
        case FARMLAND:
          b = 0.6f;
          break;
        case WOOD:
        case PLANKS:
        case WORKBENCH:
          b = 1.5f;
          break;
        case STONE:
        case COBBLE:
        case FURNACE:
        case ORE_COAL:
        case ORE_IRON:
        case ORE_GOLD:
        case ORE_DIAMOND:
        case ORE_EMERALD:
        case ORE_LAPIS:
        case ORE_REDSTONE:
        case ORE_QUARTZ:
          b = 2.0f;
          break;
        case BLOCK_COAL:
        case BLOCK_IRON:
        case BLOCK_GOLD:
        case BLOCK_DIAMOND:
        case BLOCK_EMERALD:
        case BLOCK_LAPIS:
        case BLOCK_REDSTONE:
        case BLOCK_QUARTZ:
          b = 5.0f;
          break;
        case OBSIDIAN:
          b = 10.0f;
          break;
        case GLASS:
          b = 0.3f;
          break;
        case BEDROCK:
        case PORTAL:
          return -1.0f;
        case LAVA:
        case LAVA_FLOW:
        case WATER:
        case WATER_FLOW:
          return 100.0f;
        default:
          b = 1.0f;
      }
      if ((t == WOOD_PICKAXE
              || t == STONE_PICKAXE
              || t == IRON_PICKAXE
              || t == GOLD_PICKAXE
              || t == DIAMOND_PICKAXE)
          && (id == STONE
              || id == COBBLE
              || id == FURNACE
              || id == ORE_COAL
              || id == ORE_IRON
              || id == ORE_GOLD
              || id == ORE_DIAMOND
              || id == ORE_EMERALD
              || id == ORE_LAPIS
              || id == ORE_REDSTONE
              || id == ORE_QUARTZ
              || id == OBSIDIAN
              || id == BLOCK_COAL
              || id == BLOCK_IRON
              || id == BLOCK_GOLD
              || id == BLOCK_DIAMOND
              || id == BLOCK_EMERALD
              || id == BLOCK_LAPIS
              || id == BLOCK_REDSTONE
              || id == BLOCK_QUARTZ)) {
        float m =
            (t == IRON_PICKAXE)
                ? 4.0f
                : (t == GOLD_PICKAXE)
                    ? 6.0f
                    : (t == DIAMOND_PICKAXE) ? 8.0f : (t == STONE_PICKAXE) ? 3.0f : 2.0f;
        return b / m;
      }
      if ((t == WOOD_AXE || t == STONE_AXE || t == IRON_AXE || t == GOLD_AXE || t == DIAMOND_AXE)
          && (id == WOOD || id == PLANKS || id == WORKBENCH)) {
        float m =
            (t == STONE_AXE)
                ? 3.0f
                : (t == IRON_AXE)
                    ? 4.0f
                    : (t == GOLD_AXE) ? 6.0f : (t == DIAMOND_AXE) ? 8.0f : 2.0f;
        return b / m;
      }
      if ((t == WOOD_SHOVEL
              || t == STONE_SHOVEL
              || t == IRON_SHOVEL
              || t == GOLD_SHOVEL
              || t == DIAMOND_SHOVEL)
          && (id == DIRT || id == GRASS || id == SAND || id == GRAVEL)) {
        float m =
            (t == STONE_SHOVEL)
                ? 3.0f
                : (t == IRON_SHOVEL)
                    ? 4.0f
                    : (t == GOLD_SHOVEL) ? 6.0f : (t == DIAMOND_SHOVEL) ? 8.0f : 2.0f;
        return b / m;
      }
      if ((t == WOOD_SWORD
              || t == STONE_SWORD
              || t == IRON_SWORD
              || t == GOLD_SWORD
              || t == DIAMOND_SWORD)
          && (id == LEAVES)) return b / 1.5f;
      return b;
    }

    private int getPickaxeLevel(byte t) {
      if (t == WOOD_PICKAXE) return 1;
      if (t == STONE_PICKAXE) return 2;
      if (t == IRON_PICKAXE) return 3;
      if (t == DIAMOND_PICKAXE) return 4;
      if (t == GOLD_PICKAXE) return 1;
      return 0;
    }

    private byte getDropItem(byte b, byte t) {
      if (b == GRASS_PATH) return DIRT;
      if (b == BED_BLOCK) return BED_BLOCK;
      if (b == IRON_BARS) return IRON_BARS;
      if (b == GLASS_PANE) return b;
      if (creativeMode) return AIR;
      int lvl = getPickaxeLevel(t);
      if (b == STONE) {
        return (lvl >= 1) ? COBBLE : AIR;
      }
      if (b == ORE_COAL) {
        return (lvl >= 1) ? COAL : AIR;
      }
      if (b == ORE_IRON) {
        return (lvl >= 2) ? ORE_IRON : AIR;
      }
      if (b == ORE_LAPIS) {
        return (lvl >= 2) ? LAPIS : AIR;
      }
      if (b == ORE_GOLD) {
        return (lvl >= 3) ? ORE_GOLD : AIR;
      }
      if (b == ORE_REDSTONE) {
        return (lvl >= 3) ? REDSTONE : AIR;
      }
      if (b == ORE_DIAMOND) {
        return (lvl >= 3) ? DIAMOND : AIR;
      }
      if (b == ORE_EMERALD) {
        return (lvl >= 3) ? EMERALD : AIR;
      }
      if (b == ORE_QUARTZ) {
        return (lvl >= 1) ? QUARTZ : AIR;
      }
      if (b == OBSIDIAN) {
        return (lvl >= 4) ? OBSIDIAN : AIR;
      }
      if (b == BLOCK_COAL || b == BLOCK_REDSTONE || b == BLOCK_QUARTZ) {
        return (lvl >= 1) ? b : AIR;
      }
      if (b == BLOCK_IRON || b == BLOCK_LAPIS) {
        return (lvl >= 2) ? b : AIR;
      }
      if (b == BLOCK_GOLD || b == BLOCK_DIAMOND || b == BLOCK_EMERALD) {
        return (lvl >= 3) ? b : AIR;
      }
      if (b == GRASS || b == FARMLAND) return DIRT;
      if (b == LEAVES
          || b == LEAVES_BIRCH
          || b == LEAVES_SPRUCE
          || b == LEAVES_JUNGLE
          || b == LEAVES_ACACIA
          || b == LEAVES_DARK_OAK) {
        return (t == SHEARS) ? b : AIR;
      }
      if (b == SHORT_GRASS || b == PLANT_TALL_GRASS) {
        return (t == SHEARS) ? b : AIR;
      }
      if (b == GLASS) return AIR;
      return b;
    }

    private void calculateFlow(int x, int y, int z, float[] r) {
      r[0] = 0;
      r[1] = 0;
      byte b = getBlock(x, y, z);
      if (!isWater(b)) return;
      int d = getData(x, y, z);
      afc(x, y, z - 1, d, 0, -1, r);
      afc(x, y, z + 1, d, 0, 1, r);
      afc(x - 1, y, z, d, -1, 0, r);
      afc(x + 1, y, z, d, 1, 0, r);
      float l = (float) Math.sqrt(r[0] * r[0] + r[1] * r[1]);
      if (l > 0.01f) {
        r[0] /= l;
        r[1] /= l;
      }
    }

    private void afc(int x, int y, int z, int d, int dx, int dz, float[] r) {
      byte n = getBlock(x, y, z);
      int nd = getData(x, y, z);
      if (cf(n) && !isWater(n)) {
        r[0] += dx * 2.0f;
        r[1] += dz * 2.0f;
        return;
      }
      if (isWater(n)) {
        float w = (float) (nd - d);
        r[0] += dx * w;
        r[1] += dz * w;
      }
    }

    private boolean isDoor(byte b) {
      return b == WOOD_DOOR;
    }

    private int yawToDir() {
      int dir = (int) Math.floor((ry + 45.0f) / 90.0f) & 3;
      return dir;
    }

    private void placeDoorAt(int x, int y, int z) {
      if (y < 0 || y >= WORLD_H - 1) return;
      if (getBlock(x, y, z) != AIR) return;
      if (getBlock(x, y + 1, z) != AIR) return;
      int dir = yawToDir();
      int data = dir;
      setBlockAndDirty(x, y, z, WOOD_DOOR);
      setBlockAndDirty(x, y + 1, z, WOOD_DOOR);
      setData(x, y, z, data);
      setData(x, y + 1, z, data | 8);
      markChunkDirtyAt(x, z);
    }

    private void toggleDoor(int x, int y, int z) {
      if (getBlock(x, y, z) != WOOD_DOOR) return;
      int d = getData(x, y, z) & 0xFF;
      if ((d & 8) != 0) {
        y -= 1;
        d = getData(x, y, z) & 0xFF;
      }
      int nd = d ^ 4;
      setData(x, y, z, nd);
      setData(x, y + 1, z, nd | 8);
      markChunkDirtyAt(x, z);
    }

    private boolean isHydrated(int x, int y, int z) {
      for (int dx = -4; dx <= 4; dx++) {
        for (int dz = -4; dz <= 4; dz++) {
          byte b = getBlock(x + dx, y, z + dz);
          if (isWater(b)) return true;
        }
      }
      return false;
    }

    private float getBlockTop(int x, int y, int z) {
      byte b = getBlock(x, y, z);
      if (b == 0) return y;
      if (b == GRASS_PATH) return y + 0.9375f;
      if (b == SLAB_COBBLE || b == SLAB_OAK || b == REDSTONE) {
        int d = getData(x, y, z);
        if ((d & 3) == 0) return y + 0.5f;
        return y + 1.0f;
      }
      if (b == FENCE
          || b == NETHER_FENCE
          || b == IRON_BARS
          || b == GLASS_PANE
          || b == COBBLE
          || b == STONE) {
        return y + 1.0f;
      }
      if (b >= CARPET_BLACK && b <= CARPET_WHITE) return y + 0.0625f;
      if (b == SNOW_LAYER) return y + 0.125f;
      if (b == BED_BLOCK) return y + 0.5625f;
      if (isSolid(x, y, z)) return y + 1.0f;
      return y;
    }

    private void updateGame(int dt) {
      updateTimeOfDay(dt);
      for (int i = chatLog.size() - 1; i >= 0; i--) {
        ChatMsg cm = (ChatMsg) chatLog.elementAt(i);
        cm.timer -= (dt / 30);
        if (cm.timer <= 0) chatLog.removeElementAt(i);
      }
      checkLightInit();
      if (hotbar[selectedSlot].id == BREAD && k_1 && !creativeMode && !spectatorMode) {
        eatTimer += dt;
        if (eatTimer >= 1600) {
          eatTimer = 0;
          hotbar[selectedSlot].count--;
          if (hotbar[selectedSlot].count <= 0) hotbar[selectedSlot].id = 0;
          food += 5;
          if (food > 20) food = 20;
        }
        isSwinging = true;
      } else {
        eatTimer = 0;
      }
      background.setColor(currentSkyColor);
      globalLight.setIntensity(1.0f);
      int ipx = (int) px, ipy = (int) py, ipz = (int) pz;
      boolean inPortal =
          (getBlock(ipx, ipy, ipz) == PORTAL || getBlock(ipx, ipy + 1, ipz) == PORTAL);
      if (inPortal) {
        if (!wasInPortal) {
          if (creativeMode) {
            switchDimension();
            wasInPortal = true;
            return;
          } else {
            portalTimer += dt;
            if (portalTimer > 4000) {
              switchDimension();
              portalTimer = 0;
              wasInPortal = true;
              return;
            }
          }
        }
      } else {
        wasInPortal = false;
        portalTimer = 0;
      }
      updatePrimedTNT(dt);
      cloudOffset += (0.05f * (dt / 30.0f));
      if (cloudOffset > WORLD_X) cloudOffset -= WORLD_X;
      animTime += 0.1f;
      if (!creativeMode && !spectatorMode) {
        if (food > 17 && health < 20) {
          healTimer += dt;
          if (healTimer > 4000) {
            health++;
            healTimer = 0;
            addExhaustion(0.5f);
          }
        }
        if (food <= 0) {
          healTimer += dt;
          if (healTimer > 4000) {
            health--;
            healTimer = 0;
          }
        }
      }
      int ix = (int) px, iy = (int) py, iz = (int) pz;
      byte hb = getBlock(ix, (int) (py + 1.6f), iz), fb = getBlock(ix, iy, iz);
      if (!creativeMode && !spectatorMode) {
        if (isWater(hb)) {
          air -= 1;
          if (air < 0) {
            air = -20;
            damageTimer += dt;
            if (damageTimer > 1000) {
              health--;
              damageTimer = 0;
            }
          }
        } else {
          air = 300;
          damageTimer = 0;
        }
        if (isLava(fb) || isLava(hb)) {
          damageTimer += dt;
          if (damageTimer > 500) {
            health -= 2;
            damageTimer = 0;
          }
        }
        if (health <= 0) {
          health = 20;
          food = 20;
          air = 300;
          for (int y = 0; y < WORLD_H; y++) {
            for (int x = 0; x < WORLD_X; x++) {
              setBlock(x, y, 0, BARRIER);
              setData(x, y, 0, 1);
              setBlock(x, y, WORLD_Y - 1, BARRIER);
              setData(x, y, WORLD_Y - 1, 1);
            }
            for (int z = 0; z < WORLD_Y; z++) {
              setBlock(0, y, z, BARRIER);
              setData(0, y, z, 1);
              setBlock(WORLD_X - 1, y, z, BARRIER);
              setData(WORLD_X - 1, y, z, 1);
            }
          }
          px = 128.5f;
          pz = 128.5f;
          for (int y = WORLD_H - 1; y >= 0; y--) {
            byte b = getBlock((int) px, y, (int) pz);
            if (b != AIR && !isWater(b) && !isLava(b)) {
              py = y + 2.5f;
              break;
            }
            if (isWater(b) || isLava(b)) {
              py = SEA_LEVEL + 1.5f;
              break;
            }
          }
        }
      }
      if (isSwinging) {
        handSwing += 0.4f;
        if (handSwing > 3.14f) {
          handSwing = 0;
          isSwinging = false;
        }
      }
      if (k_5 || k_fire) {
        if (dropHoldStart == 0) dropHoldStart = System.currentTimeMillis();
        long h = System.currentTimeMillis() - dropHoldStart;
        if (!droppedInitial && !spectatorMode) {
          throwItem();
          droppedInitial = true;
          isSwinging = true;
        } else if (h > 500 && !spectatorMode) {
          if (frameCount % 4 == 0) throwItem();
        }
      } else {
        dropHoldStart = 0;
        droppedInitial = false;
      }
      if (hasTarget && k_3 && !spectatorMode) {
        isSwinging = true;
        if (!isMining || miningX != targetX || miningY != targetY || miningZ != targetZ) {
          isMining = true;
          miningX = targetX;
          miningY = targetY;
          miningZ = targetZ;
          miningProgress = 0.0f;
        }
        byte b = getBlock(targetX, targetY, targetZ);
        if (isSolid(targetX, targetY, targetZ)
            || b == WOOD_DOOR
            || isCrossed(b)
            || b == WHEAT_BLOCK
            || b == SLAB_COBBLE
            || b == SLAB_OAK
            || b == REDSTONE) {
          float h = getBlockHardness(b, hotbar[selectedSlot].id);
          if (h > 0 || creativeMode) {
            if (creativeMode) miningProgress = 1.1f;
            else {
              miningProgress += (dt / 1000.0f) / h * 1.5f;
              addExhaustion(0.02f);
            }
            if (miningProgress >= 1.0f) {
              int destroyedData = getData(targetX, targetY, targetZ);
              setBlockAndDirty(targetX, targetY, targetZ, AIR);
              byte drop = getDropItem(b, hotbar[selectedSlot].id);
              int dCount = 1;
              if (b == GLOWSTONE) {
                drop = GLOWSTONE_DUST;
                dCount = 2 + new Random().nextInt(3);
              }
              if ((b == SLAB_COBBLE || b == SLAB_OAK) && destroyedData == 2) dCount = 2;
              if (b == ORE_REDSTONE && drop == REDSTONE) {
                dCount = 4 + new Random().nextInt(2);
              }
              if (b == WHEAT_BLOCK) {
                if (destroyedData >= 7) {
                  drop = WHEAT;
                  int seeds = 1 + new Random().nextInt(4);
                  drops.addElement(
                      new Drop(
                          targetX + 0.5f,
                          targetY + 0.5f,
                          targetZ + 0.5f,
                          WHEAT_SEEDS,
                          0,
                          0,
                          0,
                          seeds,
                          500));
                } else {
                  drop = WHEAT_SEEDS;
                }
              }
              if (b == GRAVEL && new Random().nextInt(10) == 0) drop = FLINT;
              if ((b == SHORT_GRASS || b == PLANT_TALL_GRASS)
                  && hotbar[selectedSlot].id != SHEARS) {
                if (new Random().nextInt(8) == 0) drop = WHEAT_SEEDS;
              }
              if (drop != AIR)
                drops.addElement(
                    new Drop(
                        targetX + 0.5f,
                        targetY + 0.5f,
                        targetZ + 0.5f,
                        drop,
                        0,
                        0,
                        0,
                        dCount,
                        500));
              isMining = false;
              miningProgress = 0;
            }
          } else {
            isMining = false;
            miningProgress = 0;
          }
        } else {
          isMining = false;
          miningProgress = 0;
        }
      } else {
        isMining = false;
        miningProgress = 0;
      }
      if (!k_up && !k_2) isSprinting = false;
      float dSec = dt / 1000.0f;
      if (dSec > 0.1f) dSec = 0.1f;
      float rs = 120.0f * dSec;
      if (k_left) ry += rs;
      if (k_right) ry -= rs;
      if (k_up) rx += rs;
      if (k_down) rx -= rs;
      if (rx > 90) rx = 90;
      if (rx < -90) rx = -90;
      float speedBase = 4.3f;
      if (isSprinting) speedBase = 5.6f;
      if (isFlying) {
        speedBase = isSprinting ? 20.0f : 10.0f;
      }
      if (spectatorMode) speedBase = 25.0f;
      float moveDist = speedBase * dSec;
      float rY = (float) Math.toRadians(ry), sY = (float) Math.sin(rY), cY = (float) Math.cos(rY);
      float dx = 0, dz = 0;
      boolean mov = false;
      if (k_2) {
        dx -= sY * moveDist;
        dz -= cY * moveDist;
        mov = true;
      }
      if (k_8) {
        dx += sY * moveDist;
        dz += cY * moveDist;
        mov = true;
      }
      if (k_4) {
        dx -= cY * moveDist;
        dz += sY * moveDist;
        mov = true;
      }
      if (k_6) {
        dx += cY * moveDist;
        dz -= sY * moveDist;
        mov = true;
      }
      if (mov) {
        walkBob += (isSprinting ? 15.0f : 9.5f) * dSec;
        addExhaustion(isSprinting ? 0.05f : 0.01f);
      } else {
        walkBob = 0;
      }
      boolean il = (isWater(fb) || isLava(fb));
      boolean onLad = (fb == LADDER || hb == LADDER);
      if (onLad) {
        onGround = true;
        fallStartY = py;
        vy = -0.05f;
        if (k_2 || k_up) {
          vy = 0.11f;
        } else if (k_8 || k_down) {
          vy = -0.11f;
        }
      }
      if (fb == WEB || hb == WEB) {
        dx *= 0.25f;
        dz *= 0.25f;
        vy *= 0.05f;
      }
      if (getBlock(ix, iy - 1, iz) == SOUL_SAND) {
        dx *= 0.4f;
        dz *= 0.4f;
      }
      if (isWater(fb)) {
        calculateFlow(ix, iy, iz, flowVec);
        float f = 0.5f * dSec;
        dx += flowVec[0] * f;
        dz += flowVec[1] * f;
      }
      if (k_0 && onGround && !il && !onLad) {
        vy = 0.42f;
        onGround = false;
        addExhaustion(0.2f);
      }
      if (isFlying) {
        vy = 0;
        float flyVert = (isSprinting ? 15.0f : 8.0f) * dSec;
        if (k_0) vy = flyVert;
        if (k_lsk) vy = -flyVert;
      } else if (il) {
        float drag = (isLava(fb)) ? 0.5f : 0.8f;
        vy *= drag;
        if (!k_0) {
          vy -= 0.5f * dSec;
          if (vy < -0.15f) vy = -0.15f;
        } else {
          vy += 0.5f * dSec;
          if (vy > 0.2f) vy = 0.2f;
        }
        onGround = true;
      } else if (!onLad) {
        if (vy == 0 && onGround) fallStartY = py;
        else if (vy > 0) fallStartY = py;
        vy -= 1.6f * dSec;
        if (vy < -1.5f) vy = -1.5f;
      }
      if (Math.abs(dx) > 0.01f) {
        if (il && isLava(fb)) dx *= 0.3f;
        if (spectatorMode || !checkCol(px + dx, py, pz)) px += dx;
        else if (mov
            && onGround
            && !checkCol(px + dx, py + 1.1f, pz)
            && checkCol(px + dx, py, pz)) {
          if (!checkCol(px + dx, py + 0.6f, pz)) {
            px += dx;
            py += 0.1f;
          } else {
            vy = 0.4f;
            px += dx;
          }
        }
      }
      if (Math.abs(dz) > 0.01f) {
        if (il && isLava(fb)) dz *= 0.3f;
        if (spectatorMode || !checkCol(px, py, pz + dz)) pz += dz;
        else if (mov
            && onGround
            && !checkCol(px, py + 1.1f, pz + dz)
            && checkCol(px, py, pz + dz)) {
          if (!checkCol(px, py + 0.6f, pz + dz)) {
            pz += dz;
            py += 0.1f;
          } else {
            vy = 0.4f;
            pz += dz;
          }
        }
      }
      float ny = py + vy;
      if (spectatorMode) {
        py = ny;
        onGround = false;
      } else {
        if (vy < 0) {
          if (checkCol(px, ny, pz)) {
            int lx = (int) px;
            int ly = (int) (py - 0.1f);
            int lz = (int) pz;
            if (getBlock(lx, ly, lz) == FARMLAND && getData(lx, ly, lz) == 0) {
              if (getBlock(lx, ly + 1, lz) == WHEAT_BLOCK) {
                setBlockAndDirty(lx, ly + 1, lz, AIR);
                drops.addElement(
                    new Drop(lx + 0.5f, ly + 1.5f, lz + 0.5f, WHEAT_SEEDS, 0, 0.1f, 0, 1, 500));
              }
              setBlockAndDirty(lx, ly, lz, DIRT);
            }
            float fd = fallStartY - py;
            if (fd > 3.0f && !il && !creativeMode) {
              int dmg = (int) (fd - 3.0f);
              if (dmg > 0) health -= dmg;
            }
            float bestY = (float) Math.floor(py);
            int bMinX = (int) Math.floor(px - 0.3f), bMaxX = (int) Math.floor(px + 0.3f);
            int bMinZ = (int) Math.floor(pz - 0.3f), bMaxZ = (int) Math.floor(pz + 0.3f);
            int bMinY = (int) Math.floor(ny), bMaxY = (int) Math.floor(py + 0.6f);
            for (int bx = bMinX; bx <= bMaxX; bx++) {
              for (int bz = bMinZ; bz <= bMaxZ; bz++) {
                for (int by = bMinY; by <= bMaxY; by++) {
                  if (blockAabbIntersects(
                      px - 0.3f, ny, pz - 0.3f, px + 0.3f, ny + 1.8f, pz + 0.3f, bx, by, bz)) {
                    float top = getBlockTop(bx, by, bz);
                    if (top > bestY) bestY = top;
                  }
                }
              }
            }
            py = bestY;
            if (py % 1.0f < 0.05f) py += 0.001f;
            vy = 0;
            onGround = true;
            fallStartY = py;
          } else {
            py = ny;
            onGround = false;
          }
        } else if (vy > 0) {
          boolean headHit = false;
          float headY = ny + 1.8f;
          int hY = (int) Math.floor(headY);
          int minX = (int) Math.floor(px - 0.3f), maxX = (int) Math.floor(px + 0.3f);
          int minZ = (int) Math.floor(pz - 0.3f), maxZ = (int) Math.floor(pz + 0.3f);
          for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
              if (isSolid(cx, hY, cz)) headHit = true;
            }
          }
          if (headHit && !onLad) {
            vy = 0;
            py = (float) hY - 1.8f - 0.01f;
          } else {
            py = ny;
            onGround = false;
          }
        }
      }
      if (py < -5 && !spectatorMode) health = 0;
      for (int i = fallingBlocks.size() - 1; i >= 0; i--) {
        FallingBlock f = (FallingBlock) fallingBlocks.elementAt(i);
        f.vy -= 1.0f * dSec;
        if (getBlock((int) f.x, (int) f.y, (int) f.z) == WEB) f.vy *= 0.1f;
        f.y += f.vy;
        if (isSolid((int) f.x, (int) f.y, (int) f.z)) {
          f.y -= f.vy;
          int bx = (int) f.x;
          int by = (int) f.y;
          int bz = (int) f.z;
          byte tgt = getBlock(bx, by, bz);
          if (tgt == WEB
              || tgt == FLOWER_YELLOW
              || tgt == FLOWER_RED
              || tgt == MUSHROOM_RED
              || tgt == MUSHROOM_BROWN
              || tgt == DEAD_BUSH) {
            drops.addElement(new Drop(f.x, f.y, f.z, f.type, 0, 0, 0, 1, 500));
          } else {
            if (tgt == PLANT_TALL_GRASS) {
              setBlockAndDirty(bx, by, bz, AIR);
            }
            setBlockAndDirty(bx, by, bz, f.type);
          }
          fallingBlocks.removeElementAt(i);
        } else if (f.y < -5) {
          fallingBlocks.removeElementAt(i);
        }
      }
      for (int i = drops.size() - 1; i >= 0; i--) {
        Drop d = (Drop) drops.elementAt(i);
        if (d.pickupTimer > 0) d.pickupTimer -= dt;
        int dbx = (int) d.x, dby = (int) d.y, dbz = (int) d.z;
        byte dBl = getBlock(dbx, dby, dbz);
        if (isWater(dBl)) {
          d.vy += 0.05f;
          if (d.vy > 0.1f) d.vy = 0.1f;
          d.vx *= 0.8f;
          d.vz *= 0.8f;
          calculateFlow(dbx, dby, dbz, flowVec);
          d.vx += flowVec[0] * 0.02f;
          d.vz += flowVec[1] * 0.02f;
        } else d.vy -= 1.0f * dSec;
        float nY = d.y + d.vy;
        int bx = (int) d.x, bz = (int) d.z, by = (int) Math.floor(nY);
        if (isSolid(bx, by, bz)) {
          d.y = (float) (by + 1) + 0.01f;
          d.vy = 0;
          d.vx *= 0.6f;
          d.vz *= 0.6f;
        } else {
          if (isWater(dBl) && d.vy > 0 && isSolid(bx, by + 1, bz) && nY > by + 0.8f) {
            d.vy = 0;
            d.y = by + 0.8f;
          } else d.y = nY;
        }
        if (!isSolid((int) (d.x + d.vx), (int) d.y, (int) d.z)) d.x += d.vx;
        else d.vx = -d.vx * 0.5f;
        if (!isSolid((int) d.x, (int) d.y, (int) (d.z + d.vz))) d.z += d.vz;
        else d.vz = -d.vz * 0.5f;
        d.rot += 5.0f;
        if (d.pickupTimer <= 0
            && (px - d.x) * (px - d.x) + (py - d.y) * (py - d.y) + (pz - d.z) * (pz - d.z) < 2.0f
            && !spectatorMode) {
          int rem = addToInventory(d.type, d.count);
          d.count = rem;
          if (d.count <= 0) {
            drops.removeElementAt(i);
            continue;
          }
        }
        if (d.pickupTimer <= 1000) {
          for (int j = i - 1; j >= 0; j--) {
            Drop o = (Drop) drops.elementAt(j);
            if (o.type == d.type && o.pickupTimer <= 1000) {
              float dSq =
                  (d.x - o.x) * (d.x - o.x) + (d.y - o.y) * (d.y - o.y) + (d.z - o.z) * (d.z - o.z);
              if (dSq < 1.0f) {
                o.count += d.count;
                drops.removeElementAt(i);
                break;
              }
            }
          }
        }
      }
      if (k_7) {
        selectedSlot = (selectedSlot - 1 + 9) % 9;
        k_7 = false;
      }
      if (k_9) {
        selectedSlot = (selectedSlot + 1) % 9;
        k_9 = false;
      }
      hasTarget = false;
      float st = 0.1f, rayX = px, rayY = py + 1.6f, rayZ = pz;
      Transform t = new Transform();
      t.postRotate(ry, 0, 1, 0);
      t.postRotate(rx, 1, 0, 0);
      float[] fv = {0, 0, -1, 1};
      t.transform(fv);
      float diX = fv[0], diY = fv[1], diZ = fv[2];
      lastX = (int) rayX;
      lastY = (int) rayY;
      lastZ = (int) rayZ;
      for (int i = 0; i < 60; i++) {
        int pbx = (int) Math.floor(rayX),
            pby = (int) Math.floor(rayY),
            pbz = (int) Math.floor(rayZ);
        rayX += diX * st;
        rayY += diY * st;
        rayZ += diZ * st;
        int bx = (int) Math.floor(rayX), by = (int) Math.floor(rayY), bz = (int) Math.floor(rayZ);
        byte tB = getBlock(bx, by, bz);
        int hI = hotbar[selectedSlot].id;
        boolean bT =
            (hI == BUCKET || hI == BUCKET_WATER || hI == BUCKET_LAVA)
                && (isWater(tB) || isLava(tB));
        if (((isSolid(bx, by, bz) || isCrossed(tB)) && !(tB == BARRIER && getData(bx, by, bz) == 1))
            || bT
            || tB == REDSTONE
            || tB == WOOD_DOOR
            || tB == WHEAT_BLOCK
            || tB == FENCE
            || tB == SLAB_COBBLE
            || tB == SLAB_OAK) {
          targetX = bx;
          targetY = by;
          targetZ = bz;
          hasTarget = true;
          lastX = pbx;
          lastY = pby;
          lastZ = pbz;
          break;
        }
      }
      if (hasTarget) {
        if (k_1 && !spectatorMode) {
          byte b = getBlock(targetX, targetY, targetZ);
          byte hId = hotbar[selectedSlot].id;
          if (b == WOOD_DOOR) {
            toggleDoor(targetX, targetY, targetZ);
            k_1 = false;
            return;
          }
          if (hId == BUCKET || hId == BUCKET_WATER || hId == BUCKET_LAVA) {
            if (isWater(b)) {
              setBlockAndDirty(targetX, targetY, targetZ, AIR);
              if (!creativeMode) hotbar[selectedSlot].id = BUCKET_WATER;
              k_1 = false;
              return;
            }
            if (isLava(b)) {
              setBlockAndDirty(targetX, targetY, targetZ, AIR);
              if (!creativeMode) hotbar[selectedSlot].id = BUCKET_LAVA;
              k_1 = false;
              return;
            }
          }
          if (hId == BUCKET_WATER
              && !isSolid(lastX, lastY, lastZ)
              && !isBoxColliding(
                  (int) (px - 0.3f), (int) py, (int) (pz - 0.3f), lastX, lastY, lastZ)) {
            setBlockAndDirty(lastX, lastY, lastZ, WATER);
            if (!creativeMode) hotbar[selectedSlot].id = BUCKET;
            k_1 = false;
            return;
          }
          if (hId == BUCKET_LAVA
              && !isSolid(lastX, lastY, lastZ)
              && !isBoxColliding(
                  (int) (px - 0.3f), (int) py, (int) (pz - 0.3f), lastX, lastY, lastZ)) {
            setBlockAndDirty(lastX, lastY, lastZ, LAVA);
            if (!creativeMode) hotbar[selectedSlot].id = BUCKET;
            k_1 = false;
            return;
          }
          if (isArmorItem(hId)) {
            for (int i = 0; i < 4; i++)
              if (isArmorCorrectSlot(hId, i) && armor[i].count == 0) {
                armor[i].id = hId;
                armor[i].count = 1;
                hotbar[selectedSlot].count--;
                if (hotbar[selectedSlot].count == 0) hotbar[selectedSlot].id = 0;
                k_1 = false;
                return;
              }
          }
          if (hId == WHEAT_SEEDS
              && b == FARMLAND
              && getBlock(targetX, targetY + 1, targetZ) == AIR) {
            setBlockAndDirty(targetX, targetY + 1, targetZ, WHEAT_BLOCK);
            setData(targetX, targetY + 1, targetZ, 0);
            if (!creativeMode) {
              hotbar[selectedSlot].count--;
              if (hotbar[selectedSlot].count <= 0) hotbar[selectedSlot].id = 0;
            }
            k_1 = false;
            return;
          }
          if ((hId == WOOD_SHOVEL
                  || hId == STONE_SHOVEL
                  || hId == IRON_SHOVEL
                  || hId == GOLD_SHOVEL
                  || hId == DIAMOND_SHOVEL)
              && (b == GRASS || b == DIRT)) {
            setBlockAndDirty(targetX, targetY, targetZ, GRASS_PATH);
            if (!creativeMode) {
              hotbar[selectedSlot].count--;
              if (hotbar[selectedSlot].count <= 0) hotbar[selectedSlot].id = 0;
            }
            k_1 = false;
            return;
          }
          if (((hId >= WOOD_HOE && hId <= DIAMOND_HOE) || hId == STONE_HOE)
              && (b == GRASS || b == DIRT)) {
            setBlockAndDirty(targetX, targetY, targetZ, FARMLAND);
            setData(targetX, targetY, targetZ, 7);
            if (!creativeMode) {
              hotbar[selectedSlot].count--;
              if (hotbar[selectedSlot].count <= 0) hotbar[selectedSlot].id = 0;
            }
            k_1 = false;
            return;
          }
          if (b == WORKBENCH) openInventory(1);
          else if (b == FURNACE) openInventory(2);
          else if (b == CHEST) openInventory(3);
          else if (b == TNT && hotbar[selectedSlot].id == FLINT_AND_STEEL) {
            isSwinging = true;
            primeTNT(targetX, targetY, targetZ);
            k_1 = false;
          } else if (hotbar[selectedSlot].id == FLINT_AND_STEEL
              && !isSolid(lastX, lastY, lastZ)
              && !isBoxColliding(
                  (int) (px - 0.3f), (int) py, (int) (pz - 0.3f), lastX, lastY, lastZ)) {
            isSwinging = true;
            if (!tryCreatePortal(lastX, lastY, lastZ)) setBlockAndDirty(lastX, lastY, lastZ, FIRE);
            if (!creativeMode) {
              hotbar[selectedSlot].count--;
              if (hotbar[selectedSlot].count <= 0) {
                hotbar[selectedSlot].id = 0;
                hotbar[selectedSlot].count = 0;
              }
            }
            k_1 = false;
          } else if ((hId == SLAB_COBBLE || hId == SLAB_OAK)) {
            isSwinging = true;
            boolean pl = false;
            int d = getData(targetX, targetY, targetZ);
            boolean hitTop = (lastY == targetY + 1 && lastX == targetX && lastZ == targetZ);
            boolean hitBottom = (lastY == targetY - 1 && lastX == targetX && lastZ == targetZ);
            if (b == hId && d != 2) {
              if (d == 0 && hitTop) {
                setData(targetX, targetY, targetZ, 2);
                markChunkDirtyAt(targetX, targetZ);
                pl = true;
              } else if (d == 1 && hitBottom) {
                setData(targetX, targetY, targetZ, 2);
                markChunkDirtyAt(targetX, targetZ);
                pl = true;
              }
            }
            if (!pl
                && !isSolid(lastX, lastY, lastZ)
                && !isBoxColliding(
                    (int) (px - 0.3f), (int) py, (int) (pz - 0.3f), lastX, lastY, lastZ)) {
              int slD = 0;
              if (lastY == targetY - 1) {
                slD = 1;
              } else if (lastY == targetY + 1) {
                slD = 0;
              } else {
                if ((rayY - targetY) > 0.5f) slD = 1;
              }
              setBlockAndDirty(lastX, lastY, lastZ, hId);
              setData(lastX, lastY, lastZ, slD);
              pl = true;
            }
            if (pl && !creativeMode) {
              hotbar[selectedSlot].count--;
              if (hotbar[selectedSlot].count <= 0) hotbar[selectedSlot].id = 0;
            }
            k_1 = false;
          } else if (hotbar[selectedSlot].id == TORCH
              && !isSolid(lastX, lastY, lastZ)
              && !isBoxColliding(
                  (int) (px - 0.3f), (int) py, (int) (pz - 0.3f), lastX, lastY, lastZ)) {
            isSwinging = true;
            int fd = 0;
            if (lastX == targetX - 1) fd = 2;
            else if (lastX == targetX + 1) fd = 1;
            else if (lastZ == targetZ - 1) fd = 4;
            else if (lastZ == targetZ + 1) fd = 3;
            boolean valid = true;
            if (fd == 0 && !isSolid(targetX, targetY, targetZ)) valid = false;
            if (valid) {
              setBlockAndDirty(lastX, lastY, lastZ, TORCH);
              setData(lastX, lastY, lastZ, fd);
              if (!creativeMode) {
                hotbar[selectedSlot].count--;
                if (hotbar[selectedSlot].count <= 0) hotbar[selectedSlot].id = 0;
              }
            }
            k_1 = false;
          } else if (hotbar[selectedSlot].id == BED_BLOCK && hotbar[selectedSlot].count > 0) {
            if (!isSolid(lastX, lastY, lastZ)) {
              int dir = yawToDir();
              int hx = lastX, hz = lastZ;
              if (dir == 0) hz--;
              else if (dir == 1) hx--;
              else if (dir == 2) hz++;
              else if (dir == 3) hx++;
              if (!isSolid(hx, lastY, hz)
                  && !isBoxColliding((int) (px - 0.3f), (int) py, (int) (pz - 0.3f), hx, lastY, hz)
                  && !isBoxColliding(
                      (int) (px - 0.3f), (int) py, (int) (pz - 0.3f), lastX, lastY, lastZ)) {
                isSwinging = true;
                setBlockAndDirty(lastX, lastY, lastZ, BED_BLOCK);
                setData(lastX, lastY, lastZ, dir);
                setBlockAndDirty(hx, lastY, hz, BED_BLOCK);
                setData(hx, lastY, hz, dir | 8);
                markChunkDirtyAt(lastX, lastZ);
                markChunkDirtyAt(hx, hz);
                if (!creativeMode) {
                  hotbar[selectedSlot].count--;
                  if (hotbar[selectedSlot].count <= 0) hotbar[selectedSlot].id = 0;
                }
                k_1 = false;
              }
            }
          } else if (!isSolid(lastX, lastY, lastZ)
              && hotbar[selectedSlot].count > 0
              && canPlace(hotbar[selectedSlot].id)
              && !isBoxColliding(
                  (int) (px - 0.3f), (int) py, (int) (pz - 0.3f), lastX, lastY, lastZ)) {
            isSwinging = true;
            if (hotbar[selectedSlot].id == NETHER_WART) {
              if (getBlock(targetX, targetY, targetZ) == SOUL_SAND)
                setBlockAndDirty(lastX, lastY, lastZ, NETHER_WART);
            } else if (hotbar[selectedSlot].id == LADDER) {
              if (!isSolid(lastX, lastY, lastZ)
                  && lastY == targetY
                  && !isBoxColliding(
                      (int) (px - 0.3f), (int) py, (int) (pz - 0.3f), lastX, lastY, lastZ)) {
                isSwinging = true;
                setBlockAndDirty(lastX, lastY, lastZ, LADDER);
                int rawD = yawToDir();
                int fixD = rawD;
                if (rawD == 0) fixD = 2;
                else if (rawD == 2) fixD = 0;
                setData(lastX, lastY, lastZ, fixD);
                markChunkDirtyAt(lastX, lastZ);
                if (!creativeMode) {
                  hotbar[selectedSlot].count--;
                  if (hotbar[selectedSlot].count <= 0) hotbar[selectedSlot].id = 0;
                }
                k_1 = false;
              }
            } else if (hotbar[selectedSlot].id == WOOD_DOOR) {
              placeDoorAt(lastX, lastY, lastZ);
            } else if (hotbar[selectedSlot].id == PLANT_TALL_GRASS) {
              if (getBlock(lastX, lastY + 1, lastZ) == AIR) {
                setBlockAndDirty(lastX, lastY, lastZ, PLANT_TALL_GRASS);
                setData(lastX, lastY, lastZ, 0);
                setBlockAndDirty(lastX, lastY + 1, lastZ, PLANT_TALL_GRASS);
                setData(lastX, lastY + 1, lastZ, 1);
              }
            } else {
              setBlockAndDirty(lastX, lastY, lastZ, hotbar[selectedSlot].id);
              if (isDirectional(hotbar[selectedSlot].id)) {
                setData(lastX, lastY, lastZ, yawToDir());
                markChunkDirtyAt(lastX, lastZ);
              }
            }
            if (!creativeMode) {
              hotbar[selectedSlot].count--;
              if (hotbar[selectedSlot].count <= 0) hotbar[selectedSlot].id = 0;
            }
          }
          if (hotbar[selectedSlot].id != BREAD) k_1 = false;
        } else {
          if ((k_1 || (k_3 && !isMining)) && hotbar[selectedSlot].id != BREAD) {
            isSwinging = true;
            k_1 = false;
            k_3 = false;
          }
        }
      }
      int boxR = 1;
      int pX = (int) Math.floor(px);
      int pY = (int) Math.floor(py);
      int pZ = (int) Math.floor(pz);
      for (int lx = pX - boxR; lx <= pX + boxR; lx++)
        for (int lz = pZ - boxR; lz <= pZ + boxR; lz++)
          for (int ly = pY - 1; ly <= pY + 1; ly++) {
            byte pb = getBlock(lx, ly, lz);
            if (pb == PLATE_STONE || pb == PLATE_OAK || pb == PLATE_GOLD || pb == PLATE_IRON) {
              boolean pressed = false;
              float pth = 0.05f;
              float bMinX = lx + pth,
                  bMaxX = lx + 1 - pth,
                  bMinZ = lz + pth,
                  bMaxZ = lz + 1 - pth,
                  bMinY = ly,
                  bMaxY = ly + 0.25f;
              if (px > bMinX && px < bMaxX && pz > bMinZ && pz < bMaxZ && py > bMinY && py < bMaxY)
                pressed = true;
              if (!pressed) {
                for (int i = 0; i < drops.size(); i++) {
                  Drop d = (Drop) drops.elementAt(i);
                  if (d.x > bMinX && d.x < bMaxX && d.z > bMinZ && d.z < bMaxZ && d.y > bMinY
                      && d.y < bMaxY) {
                    pressed = true;
                    break;
                  }
                }
              }
              int curD = getData(lx, ly, lz);
              int newD = pressed ? 1 : 0;
              if (curD != newD) {
                setData(lx, ly, lz, newD);
                markChunkDirtyAt(lx, lz);
              }
            }
          }
    }

    private void addExhaustion(float e) {
      if (creativeMode) return;
      foodExhaustion += e;
      if (foodExhaustion >= 4.0f) {
        foodExhaustion -= 4.0f;
        if (food > 0) food--;
      }
    }

    private boolean canPlace(byte id) {
      return !((id >= IRON_PICKAXE && id <= IRON_SWORD)
          || (id >= GOLD_PICKAXE && id <= GOLD_SWORD)
          || (id >= DIAMOND_PICKAXE && id <= DIAMOND_SWORD)
          || id == GLOWSTONE_DUST
          || id == QUARTZ
          || id == STICK
          || id == FLINT
          || id == COAL
          || id == CHARCOAL
          || id == IRON_INGOT
          || id == GOLD_INGOT
          || id == DIAMOND
          || id == EMERALD
          || id == LAPIS
          || id == SHEARS
          || id == WHEAT_SEEDS
          || id == WHEAT
          || id == BREAD
          || (id >= WOOD_PICKAXE && id <= WOOD_SWORD)
          || id == STONE_PICKAXE
          || id == STONE_AXE
          || id == STONE_SHOVEL
          || id == STONE_SWORD
          || id == STONE_HOE
          || (id >= WOOD_HOE && id <= DIAMOND_HOE)
          || isArmorItem(id)
          || id == FLINT_AND_STEEL
          || id == BUCKET
          || id == BUCKET_WATER
          || id == BUCKET_LAVA);
    }

    private boolean doorAabbIntersects(
        float ax0, float ay0, float az0, float ax1, float ay1, float az1, int x, int y, int z) {
      int d = getData(x, y, z) & 0xFF;
      int oy = y;
      if ((d & 8) != 0) {
        y -= 1;
        d = getData(x, y, z) & 0xFF;
      }
      int dir = d & 3;
      boolean open = (d & 4) != 0;
      if (open) return false;
      float th = 0.1875f;
      float x0 = 0.0f, x1 = 1.0f, z0 = 0.0f, z1 = 1.0f;
      if ((dir & 1) == 0) {
        if (dir == 0) {
          z0 = 1.0f - th;
        } else {
          z1 = th;
        }
      } else {
        if (dir == 1) {
          x0 = 1.0f - th;
        } else {
          x1 = th;
        }
      }
      float bx0 = x + x0, by0 = oy, bz0 = z + z0;
      float bx1 = x + x1, by1 = oy + 1.0f, bz1 = z + z1;
      return (ax1 > bx0 && ax0 < bx1 && ay1 > by0 && ay0 < by1 && az1 > bz0 && az0 < bz1);
    }

    private boolean blockAabbIntersects(
        float ax0, float ay0, float az0, float ax1, float ay1, float az1, int x, int y, int z) {
      byte b = getBlock(x, y, z);
      if (b == REDSTONE)
        return (ax1 > x && ax0 < x + 1 && ay1 > y && ay0 < y + 0.0625f && az1 > z && az0 < z + 1);
      if (b >= CARPET_BLACK && b <= CARPET_WHITE)
        return (ax1 > x && ax0 < x + 1 && ay1 > y && ay0 < y + 0.125f && az1 > z && az0 < z + 1);
      if (b == WOOD_DOOR) return doorAabbIntersects(ax0, ay0, az0, ax1, ay1, az1, x, y, z);
      if (b == LADDER) {
        int d = getData(x, y, z);
        float th = 0.125f;
        if (d == 0)
          return (ax1 > x
              && ax0 < x + 1
              && ay1 > y
              && ay0 < y + 1
              && az1 > z + 1 - th
              && az0 < z + 1);
        if (d == 1)
          return (ax1 > x + th && ax0 < x && ay1 > y && ay0 < y + 1 && az1 > z && az0 < z + 1);
        if (d == 2)
          return (ax1 > x && ax0 < x + 1 && ay1 > y && ay0 < y + 1 && az1 > z + th && az0 < z);
        if (d == 3)
          return (ax1 > x + 1
              && ax0 < x + 1 - th
              && ay1 > y
              && ay0 < y + 1
              && az1 > z
              && az0 < z + 1);
      }
      if (b == NETHER_FENCE || b == FENCE) {
        float min = 0.375f, max = 0.625f;
        float bx0 = x + min, bx1 = x + max, bz0 = z + min, bz1 = z + max;
        return (ax1 > bx0 && ax0 < bx1 && ay1 > y && ay0 < y + 1.0f && az1 > bz0 && az0 < bz1);
      }
      if (b == IRON_BARS) {
        boolean cX = isSolid(x - 1, y, z) || isSolid(x + 1, y, z);
        float th = 0.05f;
        if (cX) {
          return (ax1 > x
              && ax0 < x + 1
              && ay1 > y
              && ay0 < y + 1
              && az1 > z + 0.5f - th
              && az0 < z + 0.5f + th);
        } else {
          return (ax1 > x + 0.5f - th
              && ax0 < x + 0.5f + th
              && ay1 > y
              && ay0 < y + 1
              && az1 > z
              && az0 < z + 1);
        }
      }
      if (b == GLASS_PANE) {
        float th = 0.125f;
        float min = 0.5f - th;
        float max = 0.5f + th;
        float bx0 = x + min, bx1 = x + max, bz0 = z + min, bz1 = z + max;
        return (ax1 > bx0 && ax0 < bx1 && ay1 > y && ay0 < y + 1.0f && az1 > bz0 && az0 < bz1);
      }
      if (b == SLAB_COBBLE || b == SLAB_OAK || b == REDSTONE) {
        int d = getData(x, y, z);
        if (d == 2)
          return (ax1 > x && ax0 < x + 1 && ay1 > y && ay0 < y + 1 && az1 > z && az0 < z + 1);
        if (d == 1)
          return (ax1 > x
              && ax0 < x + 1
              && ay1 > y + 0.5f
              && ay0 < y + 1
              && az1 > z
              && az0 < z + 1);
        return (ax1 > x && ax0 < x + 1 && ay1 > y && ay0 < y + 0.5f && az1 > z && az0 < z + 1);
      }
      if (!isSolid(x, y, z)) return false;
      float bx0 = x, by0 = y, bz0 = z;
      float bx1 = x + 1.0f, by1 = y + 1.0f, bz1 = z + 1.0f;
      return (ax1 > bx0 && ax0 < bx1 && ay1 > by0 && ay0 < by1 && az1 > bz0 && az0 < bz1);
    }

    private void renderDoorSelection(Transform t, int x, int y, int z) {
      int d = getData(x, y, z) & 0xFF;
      int baseY = y;
      if ((d & 8) != 0) {
        baseY = y - 1;
        d = getData(x, baseY, z) & 0xFF;
      }
      int dir = d & 3;
      boolean open = (d & 4) != 0;
      float th = 0.1875f;
      float x0 = 0.0f, x1 = 1.0f, z0 = 0.0f, z1 = 1.0f;
      if (!open) {
        if ((dir & 1) == 0) {
          if (dir == 0) {
            z0 = 1.0f - th;
          } else {
            z1 = th;
          }
        } else {
          if (dir == 1) {
            x0 = 1.0f - th;
          } else {
            x1 = th;
          }
        }
      } else {
        if ((dir & 1) == 0) {
          if (dir == 0) {
            x1 = th;
          } else {
            x0 = 1.0f - th;
          }
        } else {
          if (dir == 1) {
            z0 = 1.0f - th;
          } else {
            z1 = th;
          }
        }
      }
      float sx = x1 - x0, sz = z1 - z0, ox = x0, oz = z0;
      for (int dy = 0; dy < 2; dy++) {
        t.setIdentity();
        t.postTranslate(x, baseY + dy, z);
        t.postTranslate(ox, 0.0f, oz);
        t.postScale(sx, 1.0f, sz);
        g3d.render(selMesh, t);
      }
    }

    private boolean isSolid(int x, int y, int z) {
      byte b = getBlock(x, y, z);
      if (isCrossed(b)) return false;
      if (b == BARRIER) return true;
      if (b == WOOD_DOOR
          || b == WHEAT_BLOCK
          || b == PLATE_STONE
          || b == PLATE_OAK
          || b == PLATE_GOLD
          || b == PLATE_IRON) {
        return false;
      }
      return b != AIR
          && !isWater(b)
          && !isLava(b)
          && b != FIRE
          && b != PORTAL
          && b != REDSTONE
          && b != FENCE;
    }

    private boolean checkCol(float x, float y, float z) {
      float ax0 = x - PLAYER_R,
          ax1 = x + PLAYER_R,
          ay0 = y,
          ay1 = y + 1.8f,
          az0 = z - PLAYER_R,
          az1 = z + PLAYER_R;
      int x0 = (int) Math.floor(ax0),
          x1 = (int) Math.floor(ax1),
          z0 = (int) Math.floor(az0),
          z1 = (int) Math.floor(az1),
          y0 = (int) Math.floor(ay0),
          y1 = (int) Math.floor(ay1);
      for (int xx = x0; xx <= x1; xx++)
        for (int zz = z0; zz <= z1; zz++)
          for (int yy = y0; yy <= y1; yy++)
            if (blockAabbIntersects(ax0, ay0, az0, ax1, ay1, az1, xx, yy, zz)) return true;
      return false;
    }

    private boolean isBoxColliding(int px, int py, int pz, int bx, int by, int bz) {
      float mx = this.px - PLAYER_R,
          Mx = this.px + PLAYER_R,
          my = this.py,
          My = this.py + PLAYER_H,
          mz = this.pz - PLAYER_R,
          Mz = this.pz + PLAYER_R;
      return (mx < bx + 1 && Mx > bx && my < by + 1 && My > by && mz < bz + 1 && Mz > bz);
    }

    private void renderMenu() {
      Graphics g = getGraphics();
      renderPanorama(g);
      int w = getWidth();
      int h = getHeight();
      if (logoImg != null) {
        int targetW = w - 4;
        int logoW = logoImg.getWidth();
        int logoH = logoImg.getHeight();
        if (logoW > targetW) {
          if (scaledLogo == null || lastScW != w) {
            int newH = (logoH * targetW) / logoW;
            scaledLogo = resizeImage(logoImg, targetW, newH);
            lastScW = w;
          }
          g.drawImage(scaledLogo, w / 2, 55, Graphics.HCENTER | Graphics.VCENTER);
        } else {
          g.drawImage(logoImg, w / 2, 55, Graphics.HCENTER | Graphics.VCENTER);
          scaledLogo = null;
        }
      } else {
        g.setColor(-1);
        g.drawString("J2ME Minecraft", w / 2, 40, 17);
      }
      drawBtn(g, "PLAY", w / 2, 100, menuSelection == 0);
      drawBtn(g, "SETTINGS", w / 2, 140, menuSelection == 1);
      int btnRightEdge = w / 2 + 60;
      int px = (btnRightEdge + w) / 2;
      int py = 180;
      int sz = 32;
      boolean sel = (menuSelection == 2);
      if (profileImg != null) {
        g.drawImage(profileImg, px, py + sz / 2, Graphics.HCENTER | Graphics.VCENTER);
      } else {
        g.setColor(0xFF00FF);
        g.fillRect(px - sz / 2, py, sz, sz);
      }
      g.setColor(sel ? -1 : 0);
      g.drawRect(px - sz / 2 - 2, py - 2, sz + 3, sz + 3);
      if (sel) g.drawRect(px - sz / 2 - 1, py - 1, sz + 1, sz + 1);
      g.setFont(debugFont);
      g.setColor(0x888888);
      g.drawString("v0.4.0-alpha", 2, h - 2, 36);
      g.drawString("Mojang AB", w - 2, h - 2, 40);
    }

    private void renderProfile() {
      Graphics g = getGraphics();
      renderPanorama(g);
      g.setColor(-1);
      g.drawString("PROFILE", getWidth() / 2, 20, 17);
      g.drawString("Name: " + MinecraftMIDlet.this.playerName, getWidth() / 2, 60, 17);
      drawBtn(g, "Nickname", getWidth() / 2, 100, menuSelection == 0);
      drawBtn(g, "Back", getWidth() / 2, 140, menuSelection == 1);
    }

    private void renderSetup() {
      if (gameState != 7) return;
      Graphics g = getGraphics();
      renderPanorama(g);
      g.setColor(-1);
      g.drawString("WORLD SETTINGS", getWidth() / 2, 20, 17);
      drawBtn(
          g,
          "Mode: " + (setupMode == 0 ? "Survival" : "Creative"),
          getWidth() / 2,
          80,
          menuSelection == 0);
      drawBtn(
          g,
          "TYPE: " + (setupType == 0 ? "Default" : "Flat"),
          getWidth() / 2,
          120,
          menuSelection == 1);
      drawBtn(
          g,
          "Seed: " + (currentSeed.length() > 0 ? currentSeed : "Random"),
          getWidth() / 2,
          160,
          menuSelection == 2);
      drawBtn(g, "CREATE WORLD", getWidth() / 2, 200, menuSelection == 3);
      drawBtn(g, "Back", getWidth() / 2, 240, menuSelection == 4);
    }

    private void renderPause() {
      renderGame();
      Graphics g = getGraphics();
      for (int i = 0; i < getHeight(); i += 2) {
        g.setColor(0);
        g.drawLine(0, i, getWidth(), i);
      }
      g.setColor(-1);
      g.drawString("PAUSED", getWidth() / 2, 20, 17);
      drawBtn(g, "Resume", getWidth() / 2, 80, menuSelection == 0);
      drawBtn(g, "Settings", getWidth() / 2, 120, menuSelection == 1);
      drawBtn(g, "Quit", getWidth() / 2, 160, menuSelection == 2);
    }

    private void renderSettings() {
      Graphics g = getGraphics();
      if (world != null) {
        renderGame();
      } else {
        renderPanorama(g);
      }
      for (int i = 0; i < getHeight(); i += 2) {
        g.setColor(0);
        g.drawLine(0, i, getWidth(), i);
      }
      g.setColor(-1);
      int cx = getWidth() / 2;
      int startY = 60;
      int gap = 40;
      if (settingsPage == 0) {
        g.drawString("SETTINGS", cx, 20, 17);
        drawBtn(g, "Graphics", cx, startY, menuSelection == 0);
        drawBtn(g, "Sound", cx, startY + gap, menuSelection == 1);
        drawBtn(g, "Other", cx, startY + gap * 2, menuSelection == 2);
        drawBtn(g, "Back", cx, startY + gap * 3, menuSelection == 3);
      } else if (settingsPage == 1) {
        g.drawString("GRAPHICS", cx, 20, 17);
        int gy = 50;
        int gg = 30;
        int cL = cx - 64;
        int cR = cx + 64;
        drawBtn(g, setDrops == 0 ? "Drops: 3D" : "Drops: 2D", cL, gy, menuSelection == 0);
        drawBtn(g, setLiquid == 0 ? "Liquid: Phys" : "Liquid: Simp", cR, gy, menuSelection == 1);
        drawBtn(
            g,
            setClouds == 0 ? "Cloud: 3D" : (setClouds == 1 ? "Cloud: 2D" : "Cloud: Off"),
            cL,
            gy + gg,
            menuSelection == 2);
        drawBtn(g, setEffects == 0 ? "Fx: ON" : "Fx: OFF", cR, gy + gg, menuSelection == 3);
        drawBtn(
            g, setAnimations == 1 ? "Anim: ON" : "Anim: OFF", cL, gy + gg * 2, menuSelection == 4);
        String lStr = "Light: Off";
        if (setLight == 1) lStr = "Light: Low";
        if (setLight == 2) lStr = "Light: High";
        drawBtn(g, lStr, cR, gy + gg * 2, menuSelection == 5);
        drawBtn(g, "Chunks: " + setChunks, cL, gy + gg * 3, menuSelection == 6);
        drawBtn(g, "Back", cR, gy + gg * 3, menuSelection == 7);
      } else if (settingsPage == 2) {
        g.drawString("SOUND", cx, 20, 17);
        int by = 80;
        if (menuSelection == 0) {
          g.setColor(0x444444);
          g.fillRect(cx - 60, by, 120, 30);
          g.setColor(-1);
          g.drawRect(cx - 60, by, 120, 30);
          g.drawRect(cx - 59, by + 1, 118, 28);
        } else {
          g.setColor(0x222222);
          g.fillRect(cx - 60, by, 120, 30);
          g.setColor(0x888888);
          g.drawRect(cx - 60, by, 120, 30);
        }
        int barW = (116 * setMusic) / 100;
        g.setColor(0x00AA00);
        g.fillRect(cx - 58, by + 2, barW, 26);
        g.setColor(-1);
        g.drawString("Vol: " + setMusic + "%", cx, by + 4, 17);
        drawBtn(g, "Back", cx, 140, menuSelection == 1);
      } else if (settingsPage == 3) {
        g.drawString("OTHER", cx, 20, 17);
        drawBtn(g, showFPS ? "Show FPS: ON" : "Show FPS: OFF", cx, 80, menuSelection == 0);
        drawBtn(g, showXYZ ? "Show XYZ: ON" : "Show XYZ: OFF", cx, 120, menuSelection == 1);
        drawBtn(g, "Back", cx, 180, menuSelection == 2);
      }
    }

    private void drawBtn(Graphics g, String t, int x, int y, boolean s) {
      int w = 120, h = 30;
      g.setFont(btnFont);
      g.setColor(0x666666);
      g.fillRect(x - w / 2, y, w, h);
      g.setColor(s ? -1 : 0);
      g.drawRect(x - w / 2, y, w, h);
      if (s) g.drawRect(x - w / 2 + 1, y + 1, w - 2, h - 2);
      g.setColor(-1);
      g.drawString(t, x, y + 4, 17);
    }

    private void drawVerticalString(Graphics g, String s, int x, int y) {
      int fh = g.getFont().getHeight();
      int step = fh - 6;
      if (step < 6) step = 6;
      int totalH = s.length() * step;
      int h = getHeight();
      int startY = (h - totalH) / 2;
      if (startY < 2) startY = 2;
      for (int i = 0; i < s.length(); i++) {
        g.drawString(s.substring(i, i + 1), x, startY + i * step, 17 | 16);
      }
    }

    private void renderInventory() {
      Graphics g = getGraphics();
      g.setColor(0xC6C6C6);
      g.fillRect(0, 0, getWidth(), getHeight());
      g.setFont(invFont);
      boolean wb = (gameState == 5), fur = (gameState == 6);
      boolean nt = (wb || fur || (gameState == 8));
      int w = getWidth();
      int h = getHeight();
      int pad = 2;
      if (w < h) {
        int cols = 10;
        int colWidth = (w - 4) / cols;
        slSz = colWidth - pad;
        if (slSz < 10) slSz = 10;
        int totalW = cols * (slSz + pad);
        guiOx = (w - totalW) / 2;
      } else {
        if (nt) {
          slSz = 20;
          int gridW = 9 * (slSz + pad);
          guiOx = (w - gridW) / 2;
        } else {
          int availableH = h - 16;
          int potentialSz = (int) (availableH / 9.5) - pad;
          if (potentialSz < 16) potentialSz = 16;
          if (potentialSz > 22) potentialSz = 22;
          slSz = potentialSz;
          int gridW = 9 * (slSz + pad);
          guiOx = (w - gridW) / 2;
        }
      }
      int sz = slSz, cdX = -100, cdY = -100, cW = wb ? 3 : 2, cH = wb ? 3 : 2;
      int hotY = guiOy + (sz + pad) * 8;
      int invY = hotY - (sz + pad) * 3 - 8;
      int crX = (wb) ? guiOx + (sz + pad) * 3 : guiOx + (sz + pad) * 5;
      int crY = (wb) ? guiOy + (sz + pad) : guiOy;
      int rX = crX + (sz + pad) * cW + 16;
      int rY = crY + (sz + pad) * (cH / 2);
      if (creativeMode && !nt) {
        int mw = 9 * (sz + pad);
        int bw = mw / 2;
        g.setColor(creativeTab == 0 ? -1 : 0x888888);
        g.fillRect(guiOx, guiOy - 15, bw, 14);
        g.setColor(0);
        g.drawString("Creative", guiOx + bw / 2, guiOy - 8, Graphics.HCENTER | Graphics.VCENTER);
        g.setColor(creativeTab == 1 ? -1 : 0x888888);
        g.fillRect(guiOx + bw, guiOy - 15, mw - bw, 14);
        g.setColor(0);
        g.drawString(
            "Inventory",
            guiOx + bw + (mw - bw) / 2,
            guiOy - 8,
            Graphics.HCENTER | Graphics.VCENTER);
        if (invSection == SEC_TABS) {
          g.setColor(0xFF0000);
          g.drawRect(
              creativeTab == 0 ? guiOx : guiOx + bw,
              guiOy - 15,
              creativeTab == 0 ? bw : mw - bw,
              14);
        }
      }
      if (creativeMode && creativeTab == 0) {
        int rows = 8;
        for (int r = 0; r < rows; r++)
          for (int c = 0; c < 9; c++) {
            int idx = c + (r + libScroll) * 9;
            int x = guiOx + c * (sz + pad), y = guiOy + r * (sz + pad);
            g.setColor(0x8B8B8B);
            g.fillRect(x, y, sz, sz);
            g.setColor(0x373737);
            g.drawRect(x, y, sz, sz);
            if (idx < libItems.length) {
              Image imgLib = getTexImage(libItems[idx]);
              if (imgLib != null) {
                g.drawImage(imgLib, x + (sz - 16) / 2, y + (sz - 16) / 2, 20);
              } else {
                g.setColor(getBlockColor(libItems[idx]));
                g.fillRect(x + 2, y + 2, sz - 4, sz - 4);
              }
            }
            if (invSection == SEC_LIB && invCursorX == c && invCursorY == r) {
              g.setColor(-1);
              g.drawRect(x, y, sz, sz);
              g.drawRect(x + 1, y + 1, sz - 2, sz - 2);
              cdX = x;
              cdY = y;
            }
          }
        {
          int _sx = guiOx + 9 * (sz + pad) + 1;
          int _sy = guiOy;
          int _sw = 12;
          int _sh = 8 * (sz + pad) - 3;
          g.setColor(0x303030);
          g.fillRect(_sx, _sy, _sw, _sh);
          g.setColor(0x606060);
          g.drawRect(_sx, _sy, _sw, _sh);
          int _tr = (libItems.length + 8) / 9;
          int _vr = 8;
          int _ms = _tr - _vr;
          if (_ms < 1) _ms = 1;
          int _th = (_vr * _sh) / _tr;
          if (_th < 14) _th = 14;
          if (_th > _sh) _th = _sh;
          int _ty = _sy + (libScroll * (_sh - _th)) / _ms;
          g.setColor(0xAAAAAA);
          g.fillRect(_sx, _ty, _sw, _th);
          g.setColor(0xFFFFFF);
          g.drawRect(_sx, _ty, _sw, _th);
          if (k_0 && invSection == SEC_LIB) g.setColor(0xFFFF00);
          else g.setColor(0x000000);
          String _s = "0";
          int _tx = _sx + (_sw - invFont.stringWidth(_s)) / 2;
          int _tyT = _ty + (_th - invFont.getHeight()) / 2;
          g.drawString(_s, _tx, _tyT, 20);
        }
        for (int i = 0; i < 9; i++) {
          int x = guiOx + i * (sz + pad);
          boolean s = (invSection == SEC_HOTBAR && invCursorX == i);
          drawSlot(g, x, hotY, sz, hotbar[i], s);
          if (s) {
            cdX = x;
            cdY = hotY;
          }
        }
      } else {
        for (int i = 0; i < 9; i++) {
          int x = guiOx + i * (sz + pad);
          boolean s = (invSection == SEC_HOTBAR && invCursorX == i);
          drawSlot(g, x, hotY, sz, hotbar[i], s);
          if (s) {
            cdX = x;
            cdY = hotY;
          }
        }
        for (int r = 0; r < 3; r++)
          for (int c = 0; c < 9; c++) {
            int x = guiOx + c * (sz + pad), y = invY + r * (sz + pad);
            boolean s = (invSection == SEC_INV && invCursorX == c && invCursorY == r);
            drawSlot(g, x, y, sz, inventory[c + r * 9], s);
            if (s) {
              cdX = x;
              cdY = y;
            }
          }
        if (gameState == 8) {
          int shiftY = (chestRows > 3) ? (chestRows - 3) * 22 : 0;
          int chestY = invY - shiftY;
          g.setColor(0x404040);
          g.drawString("Chest", guiOx, chestY - 20, 20);
          for (int r = 0; r < chestRows; r++) {
            for (int c = 0; c < 9; c++) {
              int idx = c + r * 9;
              if (chestInv != null && idx < chestInv.length) {
                int x = guiOx + c * (sz + pad), y = chestY + r * (sz + pad);
                boolean s = (invSection == SEC_CHEST && invCursorX == c && invCursorY == r);
                drawSlot(g, x, y, sz, chestInv[idx], s);
                if (s) {
                  cdX = x;
                  cdY = y;
                }
              }
            }
          }
        } else if (fur) {
          g.setColor(0x404040);
          g.drawString("Furnace", guiOx, guiOy, 20);
          int fx = guiOx + 50, fy = guiOy + 10;
          drawSlot(g, fx, fy, sz, furnaceIn, (invSection == SEC_FURNACE_IN));
          if (invSection == SEC_FURNACE_IN) {
            cdX = fx;
            cdY = fy;
          }
          drawSlot(g, fx + 50, fy + 20, sz, furnaceOut, (invSection == SEC_FURNACE_OUT));
          if (invSection == SEC_FURNACE_OUT) {
            cdX = fx + 50;
            cdY = fy + 20;
          }
          drawSlot(g, fx, fy + 40, sz, furnaceFuel, (invSection == SEC_FURNACE_FUEL));
          if (invSection == SEC_FURNACE_FUEL) {
            cdX = fx;
            cdY = fy + 40;
          }
          if (burnTime > 0 && burnTimeMax > 0) {
            int brn = (burnTime * 12) / burnTimeMax;
            g.setColor(0xFF4500);
            g.fillRect(fx + sz + 4, fy + 36 - brn, 4, brn);
          }
          if (cookTime > 0) {
            int wCook = (cookTime * 24) / 2000;
            g.setColor(0xFFFFFF);
            g.fillRect(fx + sz + 2, fy + 10, wCook, 6);
          }
        } else {
          if (!wb) {
            int ax = guiOx, ay = guiOy;
            for (int i = 0; i < 4; i++) {
              int y = ay + i * (sz + pad);
              boolean s = (invSection == SEC_ARMOR && invCursorY == i);
              drawSlot(g, ax, y, sz, armor[i], s);
              if (armor[i].count == 0) {
                Image bg = null;
                if (i == 0) bg = imgArmorHelmet;
                else if (i == 1) bg = imgArmorChest;
                else if (i == 2) bg = imgArmorLegs;
                else if (i == 3) bg = imgArmorBoots;
                if (bg != null) {
                  g.drawImage(bg, ax + (sz - 16) / 2, y + (sz - 16) / 2, 20);
                }
              }
              if (s) {
                cdX = ax;
                cdY = y;
              }
            }
          } else {
            g.setColor(0x404040);
            g.drawString("Crafting", guiOx, guiOy, 20);
          }
          for (int r = 0; r < cH; r++)
            for (int c = 0; c < cW; c++) {
              int x = crX + c * (sz + pad), y = crY + r * (sz + pad);
              boolean s = (invSection == SEC_CRAFT && invCursorX == c && invCursorY == r);
              drawSlot(g, x, y, sz, craft[c + r * cW], s);
              if (s) {
                cdX = x;
                cdY = y;
              }
            }
          boolean sr = (invSection == SEC_RESULT);
          drawSlot(g, rX, rY, sz, craftResult, sr);
          if (sr) {
            cdX = rX;
            cdY = rY;
          }
        }
      }
      if (creativeMode) {
        int dx = guiOx + 9 * (sz + pad);
        g.setColor((invSection == SEC_DELETE) ? 0xFFFFFF : 0x8B8B8B);
        g.fillRect(dx, hotY, sz, sz);
        g.setColor(0x373737);
        g.drawRect(dx, hotY, sz, sz);
        g.setColor(0xFF0000);
        g.drawLine(dx + 2, hotY + 2, dx + sz - 3, hotY + sz - 3);
        g.drawLine(dx + sz - 3, hotY + 2, dx + 2, hotY + sz - 3);
        if (invSection == SEC_DELETE) {
          cdX = dx;
          cdY = hotY;
        }
      }
      if (cursor.count > 0 && cdX > 0) {
        int fs = sz + 6, fx = cdX - 3, fy = cdY - 3;
        Image imgCur = getTexImage(cursor.id);
        if (imgCur != null) {
          g.drawImage(imgCur, fx + (fs - 16) / 2, fy + (fs - 16) / 2, 20);
        } else {
          g.setColor(getBlockColor(cursor.id));
          g.fillRect(fx, fy, fs, fs);
        }
        g.setColor(0);
        if (cursor.count > 1) g.drawString("" + cursor.count, fx + fs, fy + fs, 40);
      }
      if (tooltipTimer > 0 && !tooltipName.equals("")) {
        g.setColor(0x20000000);
        g.fillRect(cdX, cdY - 15, invFont.stringWidth(tooltipName) + 4, 15);
        g.setColor(-1);
        g.drawString(tooltipName, cdX + 2, cdY - 14, 20);
      }
      g.setColor(0);
      String txtInv = "Inv - # to close";
      String txtScroll = "0 to scroll";
      if (w < h) {
        g.drawString(txtInv, 2, h - 2, 36);
        if (creativeMode && invSection == SEC_LIB) g.drawString(txtScroll, w - 2, h - 2, 40);
      } else {
        if (nt) {
          g.drawString(txtInv, 2, h - 2, 36);
        } else {
          drawVerticalString(g, txtInv, guiOx / 2, 0);
          if (creativeMode && invSection == SEC_LIB) {
            drawVerticalString(g, txtScroll, w - (guiOx / 2), 0);
          }
        }
      }
    }

    private void drawSlot(Graphics g, int x, int y, int sz, Slot s, boolean sel) {
      g.setColor(sel ? 0xFFFFFF : 0x8B8B8B);
      g.fillRect(x, y, sz, sz);
      g.setColor(0x373737);
      g.drawRect(x, y, sz, sz);
      if (s != null && s.count > 0) {
        Image imgSlot = getTexImage(s.id);
        if (imgSlot != null) {
          g.drawImage(imgSlot, x + (sz - 16) / 2, y + (sz - 16) / 2, 20);
        } else {
          g.setColor(getBlockColor(s.id));
          g.fillRect(x + 2, y + 2, sz - 4, sz - 4);
        }
        if (s.count > 1) {
          g.setColor(0);
          g.drawString("" + s.count, x + sz - 2, y + sz - 2, 40);
        }
      }
    }

    private int getArmorPoints() {
      int p = 0;
      for (int i = 0; i < 4; i++) {
        if (armor[i].count > 0) {
          int id = armor[i].id;
          if (id == HELMET_IRON || id == HELMET_GOLD) p += 2;
          else if (id == HELMET_DIAMOND) p += 3;
          else if (id == CHESTPLATE_IRON || id == CHESTPLATE_GOLD) p += 6;
          else if (id == CHESTPLATE_DIAMOND) p += 8;
          else if (id == LEGGINGS_IRON || id == LEGGINGS_GOLD) p += 5;
          else if (id == LEGGINGS_DIAMOND) p += 6;
          else if (id == BOOTS_IRON || id == BOOTS_GOLD) p += 2;
          else if (id == BOOTS_DIAMOND) p += 3;
        }
      }
      return p;
    }

    private void renderGame() {
      Graphics g = getGraphics();
      g3d.bindTarget(g);
      g3d.clear(background);
      tCam.setIdentity();
      tCam.postTranslate(px, py + 1.6f, pz);
      tCam.postRotate(ry, 0, 1, 0);
      tCam.postRotate(rx, 1, 0, 0);
      g3d.setCamera(camera, tCam);
      g3d.resetLights();
      g3d.addLight(globalLight, tGlobal);
      int pCx = (int) px / CHUNK_SIZE,
          pCz = (int) pz / CHUNK_SIZE,
          rad = setChunks,
          sX = Math.max(0, pCx - rad),
          eX = Math.min(CHUNKS_X - 1, pCx + rad),
          sZ = Math.max(0, pCz - rad),
          eZ = Math.min(CHUNKS_Z - 1, pCz + rad),
          rb = 0;
      tGlobal.setIdentity();
      Vector dL = new Vector();
      for (int cx = sX; cx <= eX; cx++) {
        for (int cz = sZ; cz <= eZ; cz++) {
          Chunk c = chunks[cx + cz * CHUNKS_X];
          if (c.dirty && rb < 2) {
            c.rebuild();
            rb++;
          }
          if (!c.empty && c.mesh != null) dL.addElement(c);
        }
      }
      for (int i = 0; i < dL.size(); i++) {
        for (int j = 0; j < dL.size() - 1 - i; j++) {
          Chunk c1 = (Chunk) dL.elementAt(j);
          Chunk c2 = (Chunk) dL.elementAt(j + 1);
          float d1 =
              ((c1.cx * 16 + 8) - px) * ((c1.cx * 16 + 8) - px)
                  + ((c1.cz * 16 + 8) - pz) * ((c1.cz * 16 + 8) - pz);
          float d2 =
              ((c2.cx * 16 + 8) - px) * ((c2.cx * 16 + 8) - px)
                  + ((c2.cz * 16 + 8) - pz) * ((c2.cz * 16 + 8) - pz);
          if (d1 < d2) {
            dL.setElementAt(c2, j);
            dL.setElementAt(c1, j + 1);
          }
        }
      }
      for (int i = 0; i < dL.size(); i++) {
        g3d.render(((Chunk) dL.elementAt(i)).mesh, tGlobal);
      }
      if (setClouds != 2 && currentDim != -1) {
        float cx = cloudOffset;
        tGlobal.setIdentity();
        tGlobal.postTranslate(cx, 0, 0);
        Mesh cm = (setClouds == 0) ? cloudMesh3D : cloudMesh2D;
        if (cm != null) {
          g3d.render(cm, tGlobal);
          if (cx > 0) {
            tGlobal.setIdentity();
            tGlobal.postTranslate(cx - WORLD_X, 0, 0);
            g3d.render(cm, tGlobal);
          }
        }
      }
      if (hasTarget && selMesh != null) {
        tGlobal.setIdentity();
        tGlobal.postTranslate(targetX, targetY, targetZ);
        if (isMining && miningProgress > 0) {
          int s = (int) (miningProgress * 9.0f);
          if (s >= 0 && s < 10 && crackMesh != null) {
            crackMesh.setAppearance(0, appCracks[s]);
            g3d.render(crackMesh, tGlobal);
          }
        }
        byte tb = getBlock(targetX, targetY, targetZ);
        if (tb == WOOD_DOOR) {
          renderDoorSelection(tGlobal, targetX, targetY, targetZ);
        } else if (tb >= CARPET_BLACK && tb <= CARPET_WHITE) {
          tGlobal.postScale(1.0f, 0.125f, 1.0f);
          g3d.render(selMesh, tGlobal);
        } else if (tb == PLATE_STONE || tb == PLATE_OAK || tb == PLATE_GOLD || tb == PLATE_IRON) {
          int d = getData(targetX, targetY, targetZ);
          float h = (d == 1) ? 0.0625f : 0.125f;
          tGlobal.postScale(1.0f, h, 1.0f);
          g3d.render(selMesh, tGlobal);
        } else if (tb == GRASS_PATH) {
          tGlobal.postScale(1.0f, 0.9375f, 1.0f);
          g3d.render(selMesh, tGlobal);
        } else if (tb == REDSTONE) {
          tGlobal.postScale(1.0f, 0.0625f, 1.0f);
          g3d.render(selMesh, tGlobal);
        } else if (tb == SLAB_COBBLE || tb == SLAB_OAK) {
          int d = getData(targetX, targetY, targetZ);
          if (d == 0) {
            tGlobal.postScale(1.0f, 0.5f, 1.0f);
          } else if (d == 1) {
            tGlobal.postTranslate(0.0f, 0.5f, 0.0f);
            tGlobal.postScale(1.0f, 0.5f, 1.0f);
          }
          g3d.render(selMesh, tGlobal);
        } else if (tb == WHEAT_BLOCK) {
          float h = (getData(targetX, targetY, targetZ) + 1) / 8.0f;
          tGlobal.postScale(1.0f, h, 1.0f);
          g3d.render(selMesh, tGlobal);
        } else {
          g3d.render(selMesh, tGlobal);
        }
      }
      for (int i = 0; i < fallingBlocks.size(); i++) {
        FallingBlock f = (FallingBlock) fallingBlocks.elementAt(i);
        tGlobal.setIdentity();
        tGlobal.postTranslate(f.x - 0.5f, f.y - 0.5f, f.z - 0.5f);
        tGlobal.postScale(1.0f, 1.0f, 1.0f);
        setItemTextures(f.type);
        g3d.render(itemMesh, tGlobal);
      }
      for (int i = 0; i < drops.size(); i++) {
        Drop d = (Drop) drops.elementAt(i);
        float bob = (float) Math.sin(animTime) * 0.1f;
        tGlobal.setIdentity();
        tGlobal.postTranslate(d.x, d.y + 0.02f, d.z);
        g3d.render(shadowVB, shadowIB, appShadow, tGlobal);
        int dc = 1;
        if (d.count >= 2) dc = 2;
        if (d.count >= 33) dc = 3;
        for (int j = 0; j < dc; j++) {
          float off = j * 0.15f;
          tGlobal.setIdentity();
          tGlobal.postTranslate(d.x, d.y + 0.5f + bob + off, d.z);
          tGlobal.postRotate(d.rot + j * 20, 0, 1, 0);
          if (setDrops == 1 || isFlatItem(d.type)) {
            Texture2D tD2 = getTexByBlockId(d.type);
            if (tD2 != null) {
              appDrop.setTexture(0, tD2);
              dropFlatVB.setDefaultColor(0xFFFFFFFF);
            } else {
              appDrop.setTexture(0, texBorder);
              dropFlatVB.setDefaultColor(getBlockColor(d.type) | 0xFF000000);
            }
            g3d.render(dropFlatVB, dropFlatIB, appDrop, tGlobal);
          } else {
            tGlobal.postScale(0.4f, 0.4f, 0.4f);
            tGlobal.postTranslate(-0.5f, -0.5f, -0.5f);
            tGlobal.postScale(0.8f, 0.8f, 0.8f);
            if (isCrossed(d.type) && crossedMesh != null) {
              Texture2D tD3 = getTexByBlockId(d.type);
              if (tD3 != null) appDrop.setTexture(0, tD3);
              else appDrop.setTexture(0, texBorder);
              g3d.render(crossedMesh, tGlobal);
            } else {
              setItemTextures(d.type);
              g3d.render(itemMesh, tGlobal);
            }
          }
        }
      }
      renderHand();
      g3d.releaseTarget();
      int cw = getWidth() / 2, ch = getHeight() / 2;
      g.setColor(-1);
      g.drawLine(cw - 5, ch, cw + 5, ch);
      g.drawLine(cw, ch - 5, cw, ch + 5);
      int sz = 18, sx = (getWidth() - (9 * sz)) / 2, sy = getHeight() - 23;
      g.setFont(debugFont);
      int bY = sy - 12;
      if (!creativeMode) {
        int ap = getArmorPoints();
        int armY = bY - 10;
        for (int i = 0; i < 10; i++) {
          int ax = sx + i * 8;
          if (ap > i * 2 + 1) {
            if (imgArmorFull != null) g.drawImage(imgArmorFull, ax, armY, 0);
            else {
              g.setColor(0xEEEEEE);
              g.fillRect(ax, armY, 7, 7);
            }
          } else if (ap > i * 2) {
            if (imgArmorHalf != null) g.drawImage(imgArmorHalf, ax, armY, 0);
            else {
              g.setColor(0xEEEEEE);
              g.fillRect(ax, armY, 3, 7);
            }
          }
        }
        for (int i = 0; i < 10; i++) {
          int hx = sx + i * 8;
          if (health > i * 2 + 1) {
            if (imgHpFull != null) g.drawImage(imgHpFull, hx, bY, 0);
            else {
              g.setColor(0xFF0000);
              g.fillRect(hx, bY, 7, 7);
            }
          } else if (health > i * 2) {
            if (imgHpHalf != null) g.drawImage(imgHpHalf, hx, bY, 0);
            else {
              g.setColor(0xFF0000);
              g.fillRect(hx, bY, 3, 7);
            }
          } else {
            if (imgHpEmpty != null) g.drawImage(imgHpEmpty, hx, bY, 0);
            else {
              g.setColor(0x440000);
              g.drawRect(hx, bY, 7, 7);
            }
          }
          int fx = sx + (9 * sz) - 9 - i * 8;
          if (food > i * 2 + 1) {
            if (imgHungerFull != null) g.drawImage(imgHungerFull, fx, bY, 0);
            else {
              g.setColor(0x8B4513);
              g.fillRect(fx, bY, 7, 7);
            }
          } else if (food > i * 2) {
            if (imgHungerHalf != null) g.drawImage(imgHungerHalf, fx, bY, 0);
            else {
              g.setColor(0x8B4513);
              g.fillRect(fx + 4, bY, 3, 7);
            }
          } else {
            if (imgHungerEmpty != null) g.drawImage(imgHungerEmpty, fx, bY, 0);
            else {
              g.setColor(0x3B1503);
              g.drawRect(fx, bY, 7, 7);
            }
          }
        }
      }
      if (air < 300) {
        int aY = bY - 10;
        int av = (air + 29) / 30;
        for (int i = 0; i < 10; i++) {
          int ax = sx + (9 * sz) - 9 - i * 8;
          if (av > i) {
            if (imgBubble != null) g.drawImage(imgBubble, ax, aY, 0);
            else {
              g.setColor(0xADD8E6);
              g.fillArc(ax, aY, 7, 7, 0, 360);
            }
          } else {
            if (imgBubblePop != null) g.drawImage(imgBubblePop, ax, aY, 0);
            else {
              g.setColor(0x00008B);
              g.drawArc(ax, aY, 7, 7, 0, 360);
            }
          }
        }
      }
      for (int i = 0; i < 9; i++) {
        if (i == selectedSlot) g.setColor(-1);
        else g.setColor(0x888888);
        g.drawRect(sx + i * sz, sy, sz, sz);
        if (hotbar[i].count > 0) {
          Image imgHud = getTexImage(hotbar[i].id);
          if (imgHud != null) {
            g.drawImage(imgHud, sx + i * sz + (sz - 16) / 2, sy + (sz - 16) / 2, 20);
          } else {
            g.setColor(getBlockColor(hotbar[i].id));
            g.fillRect(sx + i * sz + 2, sy + 2, sz - 4, sz - 4);
          }
          if (hotbar[i].count > 1) {
            g.setColor(-1);
            g.drawString("" + hotbar[i].count, sx + i * sz + sz - 2, sy + sz - 2, 40);
          }
        }
      }
      g.setColor(-1);
      int hudY = 0;
      if (showXYZ) {
        g.drawString("XYZ:" + (int) px + " " + (int) py + " " + (int) pz, 2, hudY, 20);
        hudY += debugFont.getHeight();
      }
      if (showFPS) {
        g.drawString("FPS:" + fps, 2, hudY, 20);
      }
      g.setFont(debugFont);
      int chatBaseY = sy - 12;
      int drawnLines = 0;
      for (int i = chatLog.size() - 1; i >= 0; i--) {
        if (drawnLines >= 6) break;
        ChatMsg cm = (ChatMsg) chatLog.elementAt(i);
        if (cm != null) {
          if (cm.text.indexOf("Invalid command") != -1) {
            g.setColor(0xFF0000);
          } else {
            g.setColor(-1);
          }
          g.drawString(cm.text, 3, chatBaseY + 1, 20);
          chatBaseY -= 11;
          drawnLines++;
        }
      }
      if (showStructureLocator) {
        boolean track = false;
        int tx = 0, tz = 0;
        String lbl = "";
        int color = 0xFFFFFF;
        if (currentDim == 0 && locHasVil) {
          tx = locVilX;
          tz = locVilZ;
          track = true;
          lbl = "VIL";
          color = 0x00FF00;
        } else if (currentDim == -1 && locHasFort) {
          tx = locFortX;
          tz = locFortZ;
          track = true;
          lbl = "FRT";
          color = 0xFF0000;
        }
        if (track) {
          double dx = tx - px;
          double dz = tz - pz;
          double angToTarget = Math_atan2(dz, dx);
          double pRad = (ry * 3.141592653589793) / 180.0;
          double diff = angToTarget + pRad;
          int cx = 25;
          int cy = getHeight() - 25;
          int len = 15;
          int endX = cx + (int) (Math.cos(diff) * len);
          int endY = cy + (int) (Math.sin(diff) * len);
          g.setColor(color);
          g.drawArc(cx - 18, cy - 18, 36, 36, 0, 360);
          g.drawLine(cx, cy, endX, endY);
          g.fillTriangle(
              endX,
              endY,
              endX - (int) (Math.cos(diff + 0.5) * 4),
              endY - (int) (Math.sin(diff + 0.5) * 4),
              endX - (int) (Math.cos(diff - 0.5) * 4),
              endY - (int) (Math.sin(diff - 0.5) * 4));
          g.setColor(-1);
          g.setFont(debugFont);
          g.drawString(lbl, cx - 10, cy - 8, 20);
        }
      }
    }

    public void setItemTextures(byte id) {
      if (id == 0) return;
      Texture2D t;
      t = getTex(getTexName(id, 0));
      if (t == null) t = texBorder;
      appItemTop.setTexture(0, t);
      t = getTex(getTexName(id, 1));
      if (t == null) t = texBorder;
      appItemBot.setTexture(0, t);
      t = getTex(getTexName(id, 2));
      if (t == null) t = texBorder;
      appItemFront.setTexture(0, t);
      t = getTex(getTexName(id, 3));
      if (t == null) t = texBorder;
      appItemBack.setTexture(0, t);
      t = getTex(getTexName(id, 4));
      if (t == null) t = texBorder;
      appItemLeft.setTexture(0, t);
      t = getTex(getTexName(id, 5));
      if (t == null) t = texBorder;
      appItemRight.setTexture(0, t);
    }

    private boolean isFlatItem(byte id) {
      if (id >= WOOD_PICKAXE && id <= WOOD_SWORD) return true;
      if (id == STONE_PICKAXE
          || id == STONE_AXE
          || id == STONE_SHOVEL
          || id == STONE_SWORD
          || id == STONE_HOE) return true;
      if (id >= IRON_PICKAXE && id <= IRON_SWORD) return true;
      if (id >= GOLD_PICKAXE && id <= GOLD_SWORD) return true;
      if (id >= DIAMOND_PICKAXE && id <= DIAMOND_SWORD) return true;
      if (id >= WOOD_HOE && id <= DIAMOND_HOE) return true;
      if (id == SHEARS) return true;
      if (id >= HELMET_IRON && id <= BOOTS_IRON) return true;
      if (id >= HELMET_GOLD && id <= BOOTS_GOLD) return true;
      if (id >= HELMET_DIAMOND && id <= BOOTS_DIAMOND) return true;
      if (id == STICK || id == DIAMOND || id == FLINT || id == COAL || id == CHARCOAL) return true;
      if (id == IRON_INGOT || id == GOLD_INGOT || id == EMERALD || id == LAPIS) return true;
      if (id == FLINT_AND_STEEL || id == GLOWSTONE_DUST || id == QUARTZ || id == REDSTONE)
        return true;
      if (id == BUCKET || id == BUCKET_WATER || id == BUCKET_LAVA) return true;
      if (id == WHEAT_SEEDS || id == WHEAT || id == BREAD) return true;
      return false;
    }

    private void renderHand() {
      tHand.setIdentity();
      tHand.postTranslate(px, py + 1.6f, pz);
      tHand.postRotate(ry, 0, 1, 0);
      tHand.postRotate(rx, 1, 0, 0);
      float bX = (float) Math.sin(walkBob) * 0.05f,
          bY = (float) Math.abs(Math.cos(walkBob)) * 0.05f,
          sR = (float) Math.sin(handSwing) * 60.0f;
      tHand.postTranslate(0.5f + bX, -0.6f + bY, -1.0f);
      tHand.postRotate(-sR, 1, 0, 0);
      tHand.postRotate(45, 0, 1, 0);
      tHand.postScale(0.3f, 0.3f, 0.3f);
      int hI = hotbar[selectedSlot].id;
      if (isFlatItem((byte) hI)) {
        tHand.postRotate(-90, 0, 1, 0);
        tHand.postTranslate(-0.5f, -0.1f, 0.0f);
        tHand.postScale(-2.8f, 2.8f, 1.0f);
        Texture2D tH = getTexByBlockId((byte) hI);
        if (tH != null) appDrop.setTexture(0, tH);
        else appDrop.setTexture(0, texBorder);
        dropFlatVB.setDefaultColor(0xFFFFFFFF);
        g3d.render(dropFlatVB, dropFlatIB, appDrop, tHand);
      } else {
        if (isCrossed((byte) hI)) {
          Texture2D tH = getTexByBlockId((byte) hI);
          if (tH != null) appDrop.setTexture(0, tH);
          else appDrop.setTexture(0, texBorder);
        }
        tHand.postTranslate(-0.5f, -0.5f, -0.5f);
        tHand.postScale(0.8f, 0.8f, 0.8f);
        if (isCrossed((byte) hI) && crossedMesh != null) g3d.render(crossedMesh, tHand);
        else {
          setItemTextures((byte) hI);
          g3d.render(itemMesh, tHand);
        }
      }
    }

    private String getItemName(byte id) {
      if (id == GRASS_PATH) return "Grass Path";
      if (id == BED_BLOCK) return "Red Bed";
      if (id >= CARPET_BLACK && id <= CARPET_WHITE) {
        String[] cols = {
          "White",
          "Orange",
          "Magenta",
          "Light Blue",
          "Yellow",
          "Lime",
          "Pink",
          "Gray",
          "Light Gray",
          "Cyan",
          "Purple",
          "Blue",
          "Brown",
          "Green",
          "Red",
          "Black"
        };
        int idx = CARPET_WHITE - id;
        return cols[idx] + " Carpet";
      }
      if (id == WOOL_WHITE) return "White Wool";
      if (id == WOOL_ORANGE) return "Orange Wool";
      if (id == WOOL_MAGENTA) return "Magenta Wool";
      if (id == WOOL_LIGHT_BLUE) return "Light Blue Wool";
      if (id == WOOL_YELLOW) return "Yellow Wool";
      if (id == WOOL_LIME) return "Lime Wool";
      if (id == WOOL_PINK) return "Pink Wool";
      if (id == WOOL_GRAY) return "Gray Wool";
      if (id == WOOL_LIGHT_GRAY) return "Light Gray Wool";
      if (id == WOOL_CYAN) return "Cyan Wool";
      if (id == WOOL_PURPLE) return "Purple Wool";
      if (id == WOOL_BLUE) return "Blue Wool";
      if (id == WOOL_BROWN) return "Brown Wool";
      if (id == WOOL_GREEN) return "Green Wool";
      if (id == WOOL_RED) return "Red Wool";
      if (id == WOOL_BLACK) return "Black Wool";
      if (id == PLATE_STONE) return "Stone Plate";
      if (id == PLATE_OAK) return "Oak Plate";
      if (id == LADDER) return "Ladder";
      if (id == PLATE_GOLD) return "Gold Plate";
      if (id == PLATE_IRON) return "Iron Plate";
      if (id == IRON_BARS) return "Iron Bars";
      if (id == GLASS_PANE) return "Glass Pane";
      if (id == TORCH) return "Torch";
      if (id == SLAB_COBBLE) return "Cobblestone Slab";
      if (id == SLAB_OAK) return "Oak Slab";
      switch (id) {
        case STAIRS_WOOD:
          return "Oak Stairs";
        case STAIRS_COBBLE:
          return "Cobble Stairs";
        case FENCE:
          return "Oak Fence";
        case BOOKSHELF:
          return "Bookshelf";
        case GRASS:
          return "Grass";
        case DIRT:
          return "Dirt";
        case STONE:
          return "Stone";
        case COBBLE:
          return "Cobblestone";
        case WOOD:
          return "Log";
        case LEAVES:
          return "Leaves";
        case BEDROCK:
          return "Bedrock";
        case PLANKS:
          return "Planks";
        case WORKBENCH:
          return "Workbench";
        case STICK:
          return "Stick";
        case FURNACE:
          return "Furnace";
        case WOOD_PICKAXE:
          return "Wood Pickaxe";
        case WOOD_AXE:
          return "Wood Axe";
        case WOOD_SHOVEL:
          return "Wood Shovel";
        case WOOD_SWORD:
          return "Wood Sword";
        case SAND:
          return "Sand";
        case GRAVEL:
          return "Gravel";
        case WATER:
          return "Water";
        case WATER_FLOW:
          return "Water Flow";
        case ORE_COAL:
          return "Coal Ore";
        case ORE_IRON:
          return "Iron Ore";
        case ORE_GOLD:
          return "Gold Ore";
        case GLASS:
          return "Glass";
        case ORE_DIAMOND:
          return "Diamond Ore";
        case DIAMOND:
          return "Diamond";
        case LAVA:
          return "Lava";
        case LAVA_FLOW:
          return "Lava Flow";
        case FLINT:
          return "Flint";
        case OBSIDIAN:
          return "Obsidian";
        case COAL:
          return "Coal";
        case CHARCOAL:
          return "Charcoal";
        case IRON_INGOT:
          return "Iron Ingot";
        case GOLD_INGOT:
          return "Gold Ingot";
        case IRON_PICKAXE:
          return "Iron Pickaxe";
        case IRON_AXE:
          return "Iron Axe";
        case IRON_SHOVEL:
          return "Iron Shovel";
        case IRON_SWORD:
          return "Iron Sword";
        case GOLD_PICKAXE:
          return "Gold Pickaxe";
        case GOLD_AXE:
          return "Gold Axe";
        case GOLD_SHOVEL:
          return "Gold Shovel";
        case GOLD_SWORD:
          return "Gold Sword";
        case DIAMOND_PICKAXE:
          return "Diamond Pickaxe";
        case DIAMOND_AXE:
          return "Diamond Axe";
        case DIAMOND_SHOVEL:
          return "Diamond Shovel";
        case DIAMOND_SWORD:
          return "Diamond Sword";
        case EMERALD:
          return "Emerald";
        case LAPIS:
          return "Lapis Lazuli";
        case ORE_EMERALD:
          return "Emerald Ore";
        case ORE_LAPIS:
          return "Lapis Ore";
        case HELMET_IRON:
          return "Iron Helmet";
        case CHESTPLATE_IRON:
          return "Iron Chestplate";
        case LEGGINGS_IRON:
          return "Iron Leggings";
        case BOOTS_IRON:
          return "Iron Boots";
        case HELMET_GOLD:
          return "Gold Helmet";
        case CHESTPLATE_GOLD:
          return "Gold Chestplate";
        case LEGGINGS_GOLD:
          return "Gold Leggings";
        case BOOTS_GOLD:
          return "Gold Boots";
        case HELMET_DIAMOND:
          return "Diamond Helmet";
        case CHESTPLATE_DIAMOND:
          return "Diamond Chestplate";
        case LEGGINGS_DIAMOND:
          return "Diamond Leggings";
        case BOOTS_DIAMOND:
          return "Diamond Boots";
        case TNT:
          return "TNT";
        case FLINT_AND_STEEL:
          return "Flint and Steel";
        case PORTAL:
          return "Portal";
        case FIRE:
          return "Fire";
        case FARMLAND:
          return "Farmland";
        case WOOD_HOE:
          return "Wood Hoe";
        case IRON_HOE:
          return "Iron Hoe";
        case GOLD_HOE:
          return "Gold Hoe";
        case DIAMOND_HOE:
          return "Diamond Hoe";
        case NETHERRACK:
          return "Netherrack";
        case SOUL_SAND:
          return "Soul Sand";
        case MAGMA:
          return "Magma";
        case GLOWSTONE:
          return "Glowstone";
        case GLOWSTONE_DUST:
          return "Glowstone Dust";
        case ORE_QUARTZ:
          return "Quartz Ore";
        case QUARTZ:
          return "Nether Quartz";
        case MUSHROOM_RED:
          return "Red Mushroom";
        case MUSHROOM_BROWN:
          return "Brown Mushroom";
        case CHEST:
          return "Chest";
        case ORE_REDSTONE:
          return "Redstone Ore";
        case REDSTONE:
          return "Redstone Dust";
        case BUCKET:
          return "Bucket";
        case BUCKET_WATER:
          return "Water Bucket";
        case BUCKET_LAVA:
          return "Lava Bucket";
        case STONE_PICKAXE:
          return "Stone Pickaxe";
        case STONE_AXE:
          return "Stone Axe";
        case STONE_SHOVEL:
          return "Stone Shovel";
        case STONE_SWORD:
          return "Stone Sword";
        case STONE_HOE:
          return "Stone Hoe";
        case WOOD_DOOR:
          return "Door";
        case NETHER_BRICK:
          return "Nether Brick";
        case NETHER_FENCE:
          return "Nether Fence";
        case NETHER_STAIRS:
          return "Nether Stairs";
        case NETHER_WART:
          return "Nether Wart";
        case SANDSTONE:
          return "Sandstone";
        case CLAY:
          return "Clay";
        case ICE:
          return "Ice";
        case SNOW_BLOCK:
          return "Snow Block";
        case SHORT_GRASS:
          return "Short Grass";
        case PLANT_TALL_GRASS:
          return "Tall Grass";
        case FLOWER_YELLOW:
          return "Dandelion";
        case FLOWER_RED:
          return "Rose";
        case REEDS:
          return "Reeds";
        case CACTUS:
          return "Cactus";
        case PUMPKIN:
          return "Pumpkin";
        case JACK_O_LANTERN:
          return "Jack-o-Lantern";
        case WEB:
          return "Cobweb";
        case DEAD_BUSH:
          return "Dead Bush";
        case SNOW_LAYER:
          return "Snow Layer";
        case WOOD_BIRCH:
          return "Birch Log";
        case PLANKS_BIRCH:
          return "Birch Planks";
        case LEAVES_BIRCH:
          return "Birch Leaves";
        case WOOD_SPRUCE:
          return "Spruce Log";
        case PLANKS_SPRUCE:
          return "Spruce Planks";
        case LEAVES_SPRUCE:
          return "Spruce Leaves";
        case WOOD_JUNGLE:
          return "Jungle Log";
        case PLANKS_JUNGLE:
          return "Jungle Planks";
        case LEAVES_JUNGLE:
          return "Jungle Leaves";
        case WOOD_ACACIA:
          return "Acacia Log";
        case PLANKS_ACACIA:
          return "Acacia Planks";
        case LEAVES_ACACIA:
          return "Acacia Leaves";
        case WOOD_DARK_OAK:
          return "Dark Oak Log";
        case PLANKS_DARK_OAK:
          return "Dark Oak Planks";
        case LEAVES_DARK_OAK:
          return "Dark Oak Leaves";
        case SHEARS:
          return "Shears";
        case WHEAT_SEEDS:
          return "Wheat Seeds";
        case WHEAT_BLOCK:
          return "Crops";
        case WHEAT:
          return "Wheat";
        case BREAD:
          return "Bread";
        case BLOCK_COAL:
          return "Coal Block";
        case BLOCK_IRON:
          return "Iron Block";
        case BLOCK_GOLD:
          return "Gold Block";
        case BLOCK_REDSTONE:
          return "Redstone Block";
        case BLOCK_EMERALD:
          return "Emerald Block";
        case BLOCK_LAPIS:
          return "Lapis Block";
        case BLOCK_DIAMOND:
          return "Diamond Block";
        case BLOCK_QUARTZ:
          return "Quartz Block";
        default:
          return "Unknown";
      }
    }

    private boolean tryCreatePortal(int x, int y, int z) {
      if (checkPortalPlane(x, y, z, 1)) return true;
      if (checkPortalPlane(x, y, z, 2)) return true;
      return false;
    }

    private boolean checkPortalPlane(int x, int y, int z, int axis) {
      int yBot = y;
      while (getBlock(x, yBot - 1, z) == AIR || getBlock(x, yBot - 1, z) == FIRE) {
        yBot--;
        if (yBot < 0 || y - yBot > 21) return false;
      }
      if (getBlock(x, yBot - 1, z) != OBSIDIAN) return false;
      int dX = (axis == 1) ? 1 : 0;
      int dZ = (axis == 2) ? 1 : 0;
      int maxDim = 21;
      int posLeft = 0;
      while (true) {
        int cx = x - (posLeft + 1) * dX;
        int cz = z - (posLeft + 1) * dZ;
        byte b = getBlock(cx, yBot, cz);
        if (b == OBSIDIAN) break;
        if (b != AIR && b != FIRE) return false;
        posLeft++;
        if (posLeft > maxDim) return false;
      }
      int leftX = x - posLeft * dX;
      int leftZ = z - posLeft * dZ;
      int posRight = 0;
      while (true) {
        int cx = x + (posRight + 1) * dX;
        int cz = z + (posRight + 1) * dZ;
        byte b = getBlock(cx, yBot, cz);
        if (b == OBSIDIAN) break;
        if (b != AIR && b != FIRE) return false;
        posRight++;
        if (posRight > maxDim) return false;
      }
      int width = posLeft + posRight + 1;
      if (width < 2) return false;
      int height = 0;
      while (height <= maxDim) {
        byte b = getBlock(leftX, yBot + height, leftZ);
        if (b == OBSIDIAN) break;
        for (int w = 0; w < width; w++) {
          byte curr = getBlock(leftX + w * dX, yBot + height, leftZ + w * dZ);
          if (curr != AIR && curr != FIRE) return false;
        }
        height++;
      }
      if (height < 3 || height > maxDim) return false;
      for (int w = 0; w < width; w++)
        if (getBlock(leftX + w * dX, yBot + height, leftZ + w * dZ) != OBSIDIAN) return false;
      for (int h = 0; h < height; h++) {
        if (getBlock(leftX - dX, yBot + h, leftZ - dZ) != OBSIDIAN) return false;
        if (getBlock(leftX + width * dX, yBot + h, leftZ + width * dZ) != OBSIDIAN) return false;
      }
      for (int h = 0; h < height; h++) {
        for (int w = 0; w < width; w++) {
          int px = leftX + w * dX;
          int pz = leftZ + w * dZ;
          setBlockAndDirty(px, yBot + h, pz, PORTAL);
          setData(px, yBot + h, pz, axis);
        }
      }
      return true;
    }

    private void checkGravity(int x, int y, int z) {
      byte b = getBlock(x, y, z);
      if (b == SAND || b == GRAVEL) {
        byte d = getBlock(x, y - 1, z);
        if (d == AIR || isWater(d) || isLava(d) || d == FIRE) {
          setBlockAndDirty(x, y, z, AIR);
          fallingBlocks.addElement(new FallingBlock(x + 0.5f, y + 0.5f, z + 0.5f, b));
        }
      }
    }

    private void checkPortalBreak(int x, int y, int z, byte oldBlock) {
      if (oldBlock == PORTAL || oldBlock == OBSIDIAN) {
        removePortalIterative(x, y, z);
      }
    }

    private void removePortalIterative(int startX, int startY, int startZ) {
      Vector q = new Vector();
      int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
      for (int i = 0; i < 6; i++) {
        int nx = startX + dirs[i][0], ny = startY + dirs[i][1], nz = startZ + dirs[i][2];
        if (getBlock(nx, ny, nz) == PORTAL) {
          q.addElement(new Integer(nx + nz * WORLD_X + ny * (WORLD_X * WORLD_Y)));
        }
      }
      ignoreBreakCheck = true;
      int safety = 0;
      while (q.size() > 0 && safety < 500) {
        Integer idxObj = (Integer) q.firstElement();
        q.removeElementAt(0);
        int idx = idxObj.intValue();
        int cy = idx / (WORLD_X * WORLD_Y);
        int rem = idx % (WORLD_X * WORLD_Y);
        int cz = rem / WORLD_X;
        int cx = rem % WORLD_X;
        if (getBlock(cx, cy, cz) == PORTAL) {
          setBlockAndDirty(cx, cy, cz, AIR);
          for (int i = 0; i < 6; i++) {
            int nx = cx + dirs[i][0], ny = cy + dirs[i][1], nz = cz + dirs[i][2];
            if (getBlock(nx, ny, nz) == PORTAL) {
              q.addElement(new Integer(nx + nz * WORLD_X + ny * (WORLD_X * WORLD_Y)));
            }
          }
        }
        safety++;
      }
      ignoreBreakCheck = false;
    }

    private void switchDimension() {
      byte[] tmpW = world;
      byte[] tmpD = worldData;
      float[] tmpP = {px, py, pz, rx, ry};
      if (currentDim == 0) {
        backupWorld = tmpW;
        backupData = tmpD;
        System.arraycopy(tmpP, 0, backupPos, 0, 5);
        currentDim = -1;
        if (netherGenerated && savedNetherWorld != null) {
          world = savedNetherWorld;
          worldData = savedNetherData;
          px = savedNetherPos[0];
          py = savedNetherPos[1];
          pz = savedNetherPos[2];
          rx = savedNetherPos[3];
          ry = savedNetherPos[4];
        } else {
          generateWorld();
          netherGenerated = true;
          buildPortalAt((int) px, (int) py, (int) pz);
        }
      } else {
        savedNetherWorld = tmpW;
        savedNetherData = tmpD;
        savedNetherPos[0] = px;
        savedNetherPos[1] = py;
        savedNetherPos[2] = pz;
        savedNetherPos[3] = rx;
        savedNetherPos[4] = ry;
        currentDim = 0;
        if (backupWorld != null) {
          world = backupWorld;
          worldData = backupData;
          px = backupPos[0];
          py = backupPos[1];
          pz = backupPos[2];
          rx = backupPos[3];
          ry = backupPos[4];
        } else {
          generateWorld();
        }
      }
      wasInPortal = true;
      drops.removeAllElements();
      fallingBlocks.removeAllElements();
      for (int i = 0; i < chunks.length; i++) chunks[i].dirty = true;
      System.gc();
    }

    private byte[] savedNetherWorld, savedNetherData;
    private float[] savedNetherPos = new float[5];

    private void generateNether() {
      world = new byte[WORLD_X * WORLD_Y * WORLD_H];
      worldData = new byte[WORLD_X * WORLD_Y * WORLD_H];
      drops.removeAllElements();
      fallingBlocks.removeAllElements();
      Random r = new Random(getSeedLong());
      rand.setSeed(getSeedLong());
      int[] hm = new int[WORLD_X * WORLD_Y];
      float[][] cp = new float[CHUNKS_X + 1][CHUNKS_Z + 1];
      for (int x = 0; x <= CHUNKS_X; x++)
        for (int z = 0; z <= CHUNKS_Z; z++) cp[x][z] = r.nextFloat();
      for (int x = 0; x < WORLD_X; x++)
        for (int z = 0; z < WORLD_Y; z++) {
          int gx = x / 16, gz = z / 16;
          float tx = (x % 16) / 16.0f, tz = (z % 16) / 16.0f;
          float top = cp[gx][gz] * (1 - tx) + cp[gx + 1][gz] * tx;
          float bot = cp[gx][gz + 1] * (1 - tx) + cp[gx + 1][gz + 1] * tx;
          float val = top * (1 - tz) + bot * tz;
          hm[x + z * WORLD_X] = 10 + (int) (val * 20);
        }
      for (int x = 0; x < WORLD_X; x++)
        for (int z = 0; z < WORLD_Y; z++) {
          int h = hm[x + z * WORLD_X];
          setBlock(x, 0, z, BEDROCK);
          setBlock(x, WORLD_H - 1, z, BEDROCK);
          for (int y = 1; y < WORLD_H - 1; y++) {
            if (y < h) {
              byte b = NETHERRACK;
              if (y < 10) b = LAVA;
              else if (y == h - 1 && r.nextInt(100) < 5) b = SOUL_SAND;
              else if (r.nextInt(100) < 2) b = ORE_QUARTZ;
              setBlock(x, y, z, b);
            } else if (y > WORLD_H - 10) {
              if (r.nextInt(10) > (WORLD_H - y)) setBlock(x, y, z, NETHERRACK);
              else if (r.nextInt(100) < 2) setBlock(x, y, z, GLOWSTONE);
            }
          }
        }
      for (int y = 0; y < WORLD_H; y++) {
        for (int x = 0; x < WORLD_X; x++) {
          setBlock(x, y, 0, BARRIER);
          setData(x, y, 0, 1);
          setBlock(x, y, WORLD_Y - 1, BARRIER);
          setData(x, y, WORLD_Y - 1, 1);
        }
        for (int z = 0; z < WORLD_Y; z++) {
          setBlock(0, y, z, BARRIER);
          setData(0, y, z, 1);
          setBlock(WORLD_X - 1, y, z, BARRIER);
          setData(WORLD_X - 1, y, z, 1);
        }
      }
      py = 35;
      generateFortresses();
      for (int i = 0; i < chunks.length; i++) chunks[i].dirty = true;
    }

    private void generateFortresses() {
      int fc = 1 + rand.nextInt(2);
      for (int i = 0; i < fc; i++) {
        int mx = WORLD_X / 2, mz = WORLD_Y / 2;
        int cx = mx + (rand.nextInt(WORLD_X - 64) - (WORLD_X - 64) / 2);
        int cz = mz + (rand.nextInt(WORLD_Y - 64) - (WORLD_Y - 64) / 2);
        int cy = 28 + rand.nextInt(10);
        genFortPiece(cx, cy, cz, 0, 6, true);
        locFortX = cx;
        locFortZ = cz;
        locHasFort = true;
      }
    }

    private void genFortPiece(int x, int y, int z, int dir, int d, boolean center) {
      if (d <= 0 || x < 12 || x > WORLD_X - 12 || z < 12 || z > WORLD_Y - 12) return;
      if (center) {
        for (int px = x - 2; px <= x + 2; px++)
          for (int pz = z - 2; pz <= z + 2; pz++) {
            setBlock(px, y, pz, NETHER_BRICK);
            boolean edge = (px == x - 2 || px == x + 2 || pz == z - 2 || pz == z + 2);
            if (edge) setBlock(px, y + 1, pz, NETHER_FENCE);
            else {
              setBlock(px, y + 1, pz, AIR);
              setBlock(px, y + 2, pz, AIR);
            }
            genFortPillar(px, y - 1, pz);
          }
        if (rand.nextInt(3) == 0) genFortLoot(x, y + 1, z);
        genFortPiece(x + 3, y, z, 0, d - 1, false);
        genFortPiece(x - 3, y, z, 1, d - 1, false);
        genFortPiece(x, y, z + 3, 2, d - 1, false);
        genFortPiece(x, y, z - 3, 3, d - 1, false);
      } else {
        int len = 8 + rand.nextInt(7);
        int dx = 0, dz = 0;
        if (dir == 0) dx = 1;
        if (dir == 1) dx = -1;
        if (dir == 2) dz = 1;
        if (dir == 3) dz = -1;
        boolean br = ((rand.nextInt() & 1) != 0);
        for (int l = 0; l < len; l++) {
          int cx = x + l * dx, cz = z + l * dz;
          if (cx < 5 || cx >= WORLD_X - 5 || cz < 5 || cz >= WORLD_Y - 5) break;
          for (int w = -1; w <= 1; w++) {
            int wx = cx + (dz != 0 ? w : 0), wz = cz + (dx != 0 ? w : 0);
            setBlock(wx, y, wz, NETHER_BRICK);
            if (br) {
              if (w == -1 || w == 1) setBlock(wx, y + 1, wz, NETHER_FENCE);
              else {
                setBlock(wx, y + 1, wz, AIR);
                setBlock(wx, y + 2, wz, AIR);
              }
              genFortPillar(wx, y - 1, wz);
            } else {
              if (w == -1 || w == 1) {
                setBlock(wx, y + 1, wz, NETHER_BRICK);
                setBlock(wx, y + 2, wz, NETHER_BRICK);
                setBlock(wx, y + 3, wz, NETHER_BRICK);
                if (rand.nextInt(8) == 0) setBlock(wx, y + 1, wz, NETHER_FENCE);
              } else {
                setBlock(wx, y + 1, wz, AIR);
                setBlock(wx, y + 2, wz, AIR);
                setBlock(wx, y + 3, wz, NETHER_BRICK);
              }
            }
          }
        }
        int ex = x + len * dx, ez = z + len * dz;
        genFortPiece(ex, y, ez, dir, d - 1, true);
      }
    }

    private void genFortPillar(int x, int y, int z) {
      for (int cy = y; cy > 0; cy--) {
        byte b = getBlock(x, cy, z);
        if (b != AIR && b != LAVA && b != LAVA_FLOW) break;
        setBlock(x, cy, z, NETHER_BRICK);
      }
    }

    private void genFortLoot(int x, int y, int z) {
      Random rand = new Random();
      setBlock(x, y, z, CHEST);
      createTileEntity(x, y, z, CHEST);
      ChestTE c = getChestAt(x, y, z);
      if (c != null) {
        c.items[0].id = GOLD_INGOT;
        c.items[0].count = 2 + rand.nextInt(5);
        c.items[1].id = FLINT_AND_STEEL;
        c.items[1].count = 1;
        c.items[2].id = NETHER_WART;
        c.items[2].count = 3 + rand.nextInt(5);
        if (((rand.nextInt() & 1) != 0)) {
          c.items[3].id = OBSIDIAN;
          c.items[3].count = 2 + rand.nextInt(4);
        }
        if (rand.nextInt(5) == 0) {
          c.items[4].id = DIAMOND;
          c.items[4].count = 1;
        }
        if (rand.nextInt(3) == 0) {
          c.items[5].id = NETHER_BRICK;
          c.items[5].count = 10;
        }
      }
    }

    private void buildPortalAt(int x, int y, int z) {
      for (int dx = -2; dx <= 5; dx++)
        for (int dz = -2; dz <= 2; dz++)
          for (int dy = 0; dy < 5; dy++) {
            int nx = x + dx, ny = y + dy, nz = z + dz;
            if (nx >= 0 && nx < WORLD_X && ny > 0 && ny < WORLD_H - 1 && nz >= 0 && nz < WORLD_Y)
              setBlockAndDirty(nx, ny, nz, AIR);
          }
      for (int dx = -1; dx <= 4; dx++)
        for (int dz = -1; dz <= 1; dz++) {
          byte under = getBlock(x + dx, y - 1, z + dz);
          if (under == AIR || under == LAVA || under == NETHERRACK || under == SOUL_SAND)
            setBlockAndDirty(x + dx, y - 1, z + dz, OBSIDIAN);
        }
      for (int i = 0; i < 5; i++) {
        setBlockAndDirty(x, y + i, z, OBSIDIAN);
        setBlockAndDirty(x + 3, y + i, z, OBSIDIAN);
      }
      setBlockAndDirty(x + 1, y, z, OBSIDIAN);
      setBlockAndDirty(x + 2, y, z, OBSIDIAN);
      setBlockAndDirty(x + 1, y + 4, z, OBSIDIAN);
      setBlockAndDirty(x + 2, y + 4, z, OBSIDIAN);
      for (int dy = 1; dy < 4; dy++) {
        setBlockAndDirty(x + 1, y + dy, z, PORTAL);
        setData(x + 1, y + dy, z, 1);
        setBlockAndDirty(x + 2, y + dy, z, PORTAL);
        setData(x + 2, y + dy, z, 1);
      }
      px = x + 1.5f;
      py = y;
      pz = z + 0.5f;
    }

    private int getBlockColor(byte t) {
      if (t == BED_BLOCK) return 0xA12722;
      if (t == WOOL_WHITE) return 0xE9ECEC;
      if (t == WOOL_ORANGE) return 0xF07613;
      if (t == WOOL_MAGENTA) return 0xBD44B3;
      if (t == WOOL_LIGHT_BLUE) return 0x3AAFD9;
      if (t == WOOL_YELLOW) return 0xF8C627;
      if (t == WOOL_LIME) return 0x70B919;
      if (t == WOOL_PINK) return 0xED8DAC;
      if (t == WOOL_GRAY) return 0x3E4447;
      if (t == WOOL_LIGHT_GRAY) return 0x8E8E86;
      if (t == WOOL_CYAN) return 0x158991;
      if (t == WOOL_PURPLE) return 0x792AAC;
      if (t == WOOL_BLUE) return 0x35399D;
      if (t == WOOL_BROWN) return 0x724728;
      if (t == WOOL_GREEN) return 0x546D1B;
      if (t == WOOL_RED) return 0xA12722;
      if (t == WOOL_BLACK) return 0x141519;
      if (t == IRON_BARS) return 0xAAAAAA;
      if (t == GLASS_PANE) return 0x88FFFFFF;
      if (t == TORCH) return 0xFFD700;
      switch (t) {
        case STAIRS_WOOD:
          return 0xCD853F;
        case STAIRS_COBBLE:
          return 0xAAAAAA;
        case FENCE:
          return 0xFFFFFF;
        case BOOKSHELF:
          return 0x8B4513;
        case GRASS:
          return 0x00FF00;
        case DIRT:
          return 0x8B4513;
        case STONE:
          return 0x777777;
        case COBBLE:
          return 0xAAAAAA;
        case WOOD:
          return 0x5C3317;
        case LEAVES:
          return 0x006400;
        case BEDROCK:
          return 0x222222;
        case CLOUD:
          return 0xFFFFFF;
        case PLANKS:
          return 0xCD853F;
        case WORKBENCH:
          return 0x8B4513;
        case STICK:
          return 0xD2B48C;
        case WOOD_PICKAXE:
          return 0xDEB887;
        case WOOD_AXE:
          return 0xCD853F;
        case WOOD_SHOVEL:
          return 0xD2B48C;
        case WOOD_SWORD:
          return 0x8B4513;
        case FURNACE:
          return 0x333333;
        case SAND:
          return 0xF4A460;
        case GRAVEL:
          return 0x778899;
        case ORE_COAL:
          return 0x333333;
        case ORE_IRON:
          return 0xC0C0C0;
        case ORE_GOLD:
          return 0xFFD700;
        case ORE_DIAMOND:
          return 0x00FFFF;
        case DIAMOND:
          return 0x00FFFF;
        case WATER:
        case WATER_FLOW:
          return 0x880000FF;
        case GLASS:
          return 0x44FFFFFF;
        case LAVA:
        case LAVA_FLOW:
          return 0xFF4500;
        case FLINT:
          return 0x333333;
        case OBSIDIAN:
          return 0x1a121e;
        case COAL:
          return 0x1a1a1a;
        case CHARCOAL:
          return 0x262626;
        case IRON_INGOT:
          return 0xD8D8D8;
        case GOLD_INGOT:
          return 0xFFDF00;
        case IRON_PICKAXE:
          return 0xC0C0C0;
        case IRON_AXE:
          return 0xB8B8B8;
        case IRON_SHOVEL:
          return 0xC8C8C8;
        case IRON_SWORD:
          return 0xD0D0D0;
        case GOLD_PICKAXE:
          return 0xFFD700;
        case GOLD_AXE:
          return 0xFFDF00;
        case GOLD_SHOVEL:
          return 0xFFE44D;
        case GOLD_SWORD:
          return 0xFFD700;
        case DIAMOND_PICKAXE:
          return 0x00FFFF;
        case DIAMOND_AXE:
          return 0x00E5E5;
        case DIAMOND_SHOVEL:
          return 0x00CCCC;
        case DIAMOND_SWORD:
          return 0x00DDDD;
        case EMERALD:
          return 0x00FF00;
        case LAPIS:
          return 0x1E90FF;
        case ORE_EMERALD:
          return 0x2E8B57;
        case ORE_LAPIS:
          return 0x4169E1;
        case HELMET_IRON:
          return 0xA0A0A0;
        case CHESTPLATE_IRON:
          return 0xB0B0B0;
        case LEGGINGS_IRON:
          return 0xC0C0C0;
        case BOOTS_IRON:
          return 0xD0D0D0;
        case HELMET_GOLD:
          return 0xFFD700;
        case CHESTPLATE_GOLD:
          return 0xFFDF00;
        case LEGGINGS_GOLD:
          return 0xFFE44D;
        case BOOTS_GOLD:
          return 0xFFEA70;
        case HELMET_DIAMOND:
          return 0x00FFFF;
        case CHESTPLATE_DIAMOND:
          return 0x00E5E5;
        case LEGGINGS_DIAMOND:
          return 0x00CCCC;
        case BOOTS_DIAMOND:
          return 0x00B2B2;
        case TNT:
          return 0xFF0000;
        case FLINT_AND_STEEL:
          return 0x555555;
        case PORTAL:
          return 0x5500AA;
        case FIRE:
          return 0xFF0000;
        case FARMLAND:
          return 0x5C3317;
        case WOOD_HOE:
          return 0xCD853F;
        case IRON_HOE:
          return 0xC0C0C0;
        case GOLD_HOE:
          return 0xFFDF00;
        case DIAMOND_HOE:
          return 0x00FFFF;
        case NETHERRACK:
          return 0x6F3637;
        case SOUL_SAND:
          return 0x544033;
        case MAGMA:
          return 0x8B0000;
        case GLOWSTONE:
          return 0xFFD700;
        case GLOWSTONE_DUST:
          return 0xFFFF00;
        case ORE_QUARTZ:
          return 0xD4CBA8;
        case QUARTZ:
          return 0xFFFFFF;
        case MUSHROOM_RED:
          return 0xFF0000;
        case MUSHROOM_BROWN:
          return 0x8B4513;
        case ORE_REDSTONE:
          return 0x990000;
        case REDSTONE:
          return 0xFF0000;
        case BUCKET:
          return 0xA0A0A0;
        case BUCKET_WATER:
          return 0x0000FF;
        case BUCKET_LAVA:
          return 0xFF4500;
        case STONE_PICKAXE:
          return 0xAAAAAA;
        case STONE_AXE:
          return 0xAAAAAA;
        case STONE_SHOVEL:
          return 0xAAAAAA;
        case STONE_SWORD:
          return 0xAAAAAA;
        case STONE_HOE:
          return 0xAAAAAA;
        case WOOD_DOOR:
          return 0xCD853F;
        case NETHER_BRICK:
          return 0x2C151A;
        case NETHER_FENCE:
          return 0x2C151A;
        case NETHER_STAIRS:
          return 0x2C151A;
        case NETHER_WART:
          return 0xFF0000;
        case SANDSTONE:
          return 0xDAD29E;
        case CLAY:
          return 0x9FA4B2;
        case ICE:
          return 0x88A5C6FF;
        case SNOW_BLOCK:
          return 0xFFFFFF;
        case SHORT_GRASS:
          return 0x7C8C46;
        case PLANT_TALL_GRASS:
          return 0x557C46;
        case FLOWER_YELLOW:
          return 0xFFFF00;
        case FLOWER_RED:
          return 0xFF0000;
        case REEDS:
          return 0x9BCA6E;
        case CACTUS:
          return 0x006400;
        case PUMPKIN:
          return 0xE3901D;
        case JACK_O_LANTERN:
          return 0xFFA500;
        case WEB:
          return 0x88FFFFFF;
        case DEAD_BUSH:
          return 0x6B511F;
        case SNOW_LAYER:
          return 0xFFFFFF;
        case WOOD_BIRCH:
          return 0xE3E5E3;
        case PLANKS_BIRCH:
          return 0xDCCFA3;
        case LEAVES_BIRCH:
          return 0x80A755;
        case WOOD_SPRUCE:
          return 0x3E2714;
        case PLANKS_SPRUCE:
          return 0x735639;
        case LEAVES_SPRUCE:
          return 0x395A34;
        case WOOD_JUNGLE:
          return 0x58442A;
        case PLANKS_JUNGLE:
          return 0xA07652;
        case LEAVES_JUNGLE:
          return 0x2E8B20;
        case WOOD_ACACIA:
          return 0x6B5D53;
        case PLANKS_ACACIA:
          return 0xAF5D35;
        case LEAVES_ACACIA:
          return 0x63A946;
        case WOOD_DARK_OAK:
          return 0x34291B;
        case PLANKS_DARK_OAK:
          return 0x482E16;
        case LEAVES_DARK_OAK:
          return 0x205520;
        case SHEARS:
          return 0xCCCCCC;
        case WHEAT_SEEDS:
          return 0x808000;
        case WHEAT_BLOCK:
          return 0x00FF00;
        case WHEAT:
          return 0xFFFF00;
        case BREAD:
          return 0xD2B48C;
        case BLOCK_COAL:
          return 0x111111;
        case BLOCK_IRON:
          return 0xE0E0E0;
        case BLOCK_GOLD:
          return 0xFCEE4B;
        case BLOCK_REDSTONE:
          return 0xD90404;
        case BLOCK_EMERALD:
          return 0x00D93A;
        case BLOCK_LAPIS:
          return 0x1034A6;
        case BLOCK_DIAMOND:
          return 0x5DECF5;
        case BLOCK_QUARTZ:
          return 0xFFF8E7;
        default:
          return 0xFF00FF;
      }
    }

    private Image resizeImage(Image src, int w, int h) {
      int srcW = src.getWidth();
      int srcH = src.getHeight();
      int[] srcRgb = new int[srcW * srcH];
      src.getRGB(srcRgb, 0, srcW, 0, 0, srcW, srcH);
      int[] dstRgb = new int[w * h];
      for (int y = 0; y < h; y++) {
        int sy = y * srcH / h;
        int dy = y * w;
        int sdy = sy * srcW;
        for (int x = 0; x < w; x++) {
          int sx = x * srcW / w;
          dstRgb[dy + x] = srcRgb[sdy + sx];
        }
      }
      return Image.createRGBImage(dstRgb, w, h, true);
    }

    private class VillageGenerator {
      private int cx, cz, cy;
      private Random rnd = new Random();
      private int countSmithy = 0;
      private int countChurch = 0;
      private int countLibrary = 0;
      private int countFarm = 0;
      private int totalHouses = 0;

      public void generate() {
        int bestX = WORLD_X / 2;
        int bestZ = WORLD_Y / 2;
        int bestY = 0;
        long bestScore = -1;
        for (int i = 0; i < 50; i++) {
          int tx = 30 + (Math.abs(rnd.nextInt()) % (WORLD_X - 60));
          int tz = 30 + (Math.abs(rnd.nextInt()) % (WORLD_Y - 60));
          int centerH = getTrueGround(tx, tz);
          byte centerB = getBlock(tx, centerH, tz);
          if (centerB == 96 || centerB == 97) continue;
          long currentScore = 10000;
          int variance = 0;
          for (int ox = -4; ox <= 4; ox += 2) {
            for (int oz = -4; oz <= 4; oz += 2) {
              int gh = getTrueGround(tx + ox, tz + oz);
              variance += Math.abs(gh - centerH);
            }
          }
          currentScore -= (variance * 10);
          if (centerH >= SEA_LEVEL - 2 && centerH <= SEA_LEVEL + 2) currentScore += 500;
          if (currentScore > bestScore) {
            bestScore = currentScore;
            bestX = tx;
            bestZ = tz;
            bestY = centerH;
          }
        }
        cx = bestX;
        cz = bestZ;
        cy = bestY;
        if (cy < SEA_LEVEL) cy = SEA_LEVEL;
        MCanvas.this.locVilX = cx;
        MCanvas.this.locVilZ = cz;
        MCanvas.this.locHasVil = true;
        buildWell(cx, cy, cz);
        buildRoad(cx, cy, cz, 1, 0, 0, 12);
        buildRoad(cx, cy, cz, -1, 0, 0, 12);
        buildRoad(cx, cy, cz, 0, 0, 1, 12);
        buildRoad(cx, cy, cz, 0, 0, -1, 12);
        tryBuildBuilding(cx + 6, cy, cz + 6, 1, 0);
        tryBuildBuilding(cx - 6, cy, cz + 6, -1, 0);
        tryBuildBuilding(cx + 6, cy, cz - 6, 1, 0);
        tryBuildBuilding(cx - 6, cy, cz - 6, -1, 0);
        for (int k = 0; k < 4; k++) {
          int rx = cx + (rnd.nextInt() % 20);
          int rz = cz + (rnd.nextInt() % 20);
          tryBuildBuilding(rx, cy, rz, 0, 0);
        }
      }

      private int getTrueGround(int x, int z) {
        if (x < 0 || x >= WORLD_X || z < 0 || z >= WORLD_Y) return 0;
        for (int y = WORLD_H - 1; y >= 0; y--) {
          byte b = getBlock(x, y, z);
          if (isSolidForBuild(b)) return y;
        }
        return 0;
      }

      private boolean isSolidForBuild(byte b) {
        return b != AIR
            && b != BARRIER
            && b != LEAVES
            && b != LEAVES_BIRCH
            && b != LEAVES_SPRUCE
            && b != LEAVES_JUNGLE
            && b != PLANT_TALL_GRASS
            && b != SHORT_GRASS
            && b != FLOWER_YELLOW
            && b != FLOWER_RED
            && b != SNOW_LAYER;
      }

      private boolean isAreaClear(int x, int y, int z, int wLocal, int dLocal, int angle) {
        int wWorld = (angle % 2 == 0) ? wLocal : dLocal;
        int dWorld = (angle % 2 == 0) ? dLocal : wLocal;
        if (x < 2 || x + wWorld >= WORLD_X - 2 || z < 2 || z + dWorld >= WORLD_Y - 2) return false;
        for (int i = 0; i < wWorld; i++) {
          for (int j = 0; j < dWorld; j++) {
            int checkX = x + i;
            int checkZ = z + j;
            int floorY = getTrueGround(checkX, checkZ);
            if (Math.abs(floorY - y) > 3) return false;
            for (int k = 1; k < 5; k++) {
              byte b = getBlock(checkX, y + k, checkZ);
              if (b != AIR && isSolidForBuild(b)) return false;
            }
          }
        }
        return true;
      }

      private void buildRoad(int sx, int sy, int sz, int dx, int dz, int depth, int len) {
        if (depth > 4) return;
        int x = sx + dx * 2;
        int z = sz + dz * 2;
        int y = sy;
        for (int i = 0; i < len; i++) {
          setBlock(x, y + 1, z, AIR);
          setBlock(x, y + 2, z, AIR);
          setBlock(x, y, z, GRAVEL);
          int fy = y - 1;
          while (fy > 0) {
            byte b = getBlock(x, fy, z);
            if (isSolidForBuild(b) && b != WATER && b != WATER_FLOW) break;
            setBlock(x, fy, z, COBBLE);
            fy--;
          }
          if ((rnd.nextInt() & 0x7FFFFFFF) % 100 < 60) {
            int sideDirX = -dz;
            int sideDirZ = dx;
            if ((rnd.nextInt() & 1) != 0) {
              sideDirX = dz;
              sideDirZ = -dx;
            }
            int hx = x + sideDirX * 2;
            int hz = z + sideDirZ * 2;
            tryBuildBuilding(hx, y, hz, sideDirX, sideDirZ);
          }
          x += dx;
          z += dz;
          int ny = getTrueGround(x, z);
          if (Math.abs(ny - y) <= 1 && ny >= SEA_LEVEL) y = ny;
        }
        if ((rnd.nextInt() & 0x7FFFFFFF) % 100 < 50) {
          int turnLen = 5 + (rnd.nextInt() & 0x7FFFFFFF) % 5;
          buildRoad(x, y, z, dz, dx, depth + 1, turnLen);
          if ((rnd.nextInt() & 1) != 0) buildRoad(x, y, z, -dz, -dx, depth + 1, turnLen);
        }
      }

      private void tryBuildBuilding(int x, int y, int z, int dx, int dz) {
        int angle = 0;
        if (dx == 0 && dz == 1) angle = 0;
        else if (dx == -1 && dz == 0) angle = 1;
        else if (dx == 0 && dz == -1) angle = 2;
        else if (dx == 1 && dz == 0) angle = 3;
        angle = (angle + 2) % 4;
        int type = (rnd.nextInt() & 0x7FFFFFFF) % 14;
        if (type >= 10 && countSmithy < 1) {
          if (isAreaClear(x, y, z, 10, 7, angle)) {
            buildSmithy(x, y, z, angle);
            countSmithy++;
            totalHouses++;
          }
          return;
        }
        if (type == 9 && countChurch < 1) {
          if (isAreaClear(x, y, z, 5, 9, angle)) {
            buildChurch(x, y, z, angle);
            countChurch++;
            totalHouses++;
          }
          return;
        }
        if (type == 8) {
          if (isAreaClear(x, y, z, 5, 6, angle)) {
            buildLibrary(x, y, z, angle);
            countLibrary++;
            totalHouses++;
          }
          return;
        }
        if (type == 7 && countFarm < 3) {
          if (isAreaClear(x, y, z, 7, 9, angle)) {
            buildFarm(x, y, z, angle);
            countFarm++;
            totalHouses++;
          }
          return;
        }
        if (isAreaClear(x, y, z, 5, 5, angle)) {
          buildSmallHouse(x, y, z, angle);
          totalHouses++;
        }
      }

      private int[] getRot(int lx, int lz, int angle, int w, int d) {
        int rx = lx;
        int rz = lz;
        if (angle == 0) {
          rx = lx;
          rz = lz;
        } else if (angle == 1) {
          rx = lz;
          rz = w - 1 - lx;
        } else if (angle == 2) {
          rx = w - 1 - lx;
          rz = d - 1 - lz;
        } else if (angle == 3) {
          rx = d - 1 - lz;
          rz = lx;
        }
        return new int[] {rx, rz};
      }

      private byte getDoorData(int angle, boolean isTop) {
        if (isTop) return (byte) (8);
        int dir = 1;
        if (angle == 0) dir = 1;
        else if (angle == 1) dir = 2;
        else if (angle == 2) dir = 3;
        else if (angle == 3) dir = 0;
        return (byte) dir;
      }

      private byte getTorchData(int angle, int wallPos) {
        int dir = 4;
        for (int k = 0; k < angle; k++) {
          if (dir == 4) dir = 1;
          else if (dir == 1) dir = 3;
          else if (dir == 3) dir = 2;
          else if (dir == 2) dir = 4;
        }
        return (byte) dir;
      }

      private byte rotData(byte id, int data, int angle) {
        if (angle == 0) return (byte) data;
        if (id == STAIRS_WOOD || id == STAIRS_COBBLE) {
          int d = data & 3;
          int f = data & 4;
          for (int i = 0; i < angle; i++) {
            if (d == 0) d = 2;
            else if (d == 2) d = 1;
            else if (d == 1) d = 3;
            else if (d == 3) d = 0;
          }
          return (byte) (d | f);
        }
        if (id == FURNACE || id == CHEST || id == LADDER) {
          int d = data;
          for (int i = 0; i < angle; i++) {
            if (d == 2) d = 5;
            else if (d == 5) d = 3;
            else if (d == 3) d = 4;
            else if (d == 4) d = 2;
          }
          return (byte) d;
        }
        return (byte) data;
      }

      private void place(
          int ox,
          int oy,
          int oz,
          int lx,
          int ly,
          int lz,
          byte id,
          int data,
          int angle,
          int w,
          int d) {
        int[] r = getRot(lx, lz, angle, w, d);
        int wx = ox + r[0];
        int wz = oz + r[1];
        int wy = oy + ly;
        if (wx < 0 || wx >= WORLD_X || wz < 0 || wz >= WORLD_Y) return;
        if (ly == 0 && id != AIR) {
          setBlock(wx, wy, wz, COBBLE);
          int fy = wy - 1;
          while (fy > 0) {
            byte below = getBlock(wx, fy, wz);
            if (isSolidForBuild(below) && below != WATER && below != WATER_FLOW) {
              break;
            }
            setBlock(wx, fy, wz, COBBLE);
            fy--;
          }
        } else {
          if (id == AIR) {
            if (getBlock(wx, wy, wz) != BEDROCK) setBlock(wx, wy, wz, AIR);
            return;
          }
          setBlock(wx, wy, wz, id);
          byte finalData = (byte) data;
          if (id != WOOD_DOOR && id != TORCH) {
            finalData = rotData(id, data, angle);
          }
          setData(wx, wy, wz, finalData & 0xFF);
          if (id == CHEST) {
            createTileEntity(wx, wy, wz, id);
            ChestTE c = getChestAt(wx, wy, wz);
            if (c != null && (rnd.nextInt() & 1) != 0) {
              c.items[0].id = APPLE;
              c.items[0].count = 1;
            }
          }
          if (id == FURNACE) createTileEntity(wx, wy, wz, id);
        }
      }

      private void buildWell(int x, int y, int z) {
        for (int i = 0; i < 4; i++)
          for (int j = 0; j < 4; j++) {
            boolean isCore = (i >= 1 && i <= 2 && j >= 1 && j <= 2);
            boolean isCorner =
                (i == 0 && j == 0)
                    || (i == 3 && j == 0)
                    || (i == 0 && j == 3)
                    || (i == 3 && j == 3);
            if (isCore) {
              setBlock(x + i, y, z + j, WATER);
              setBlock(x + i, y - 1, z + j, WATER);
            } else {
              setBlock(x + i, y, z + j, COBBLE);
            }
            if (isCorner) {
              setBlock(x + i, y + 1, z + j, FENCE);
              setBlock(x + i, y + 2, z + j, FENCE);
            } else {
              setBlock(x + i, y + 1, z + j, AIR);
              setBlock(x + i, y + 2, z + j, AIR);
            }
            setBlock(x + i, y + 3, z + j, COBBLE);
          }
      }

      private void buildSmallHouse(int x, int y, int z, int ang) {
        int W = 5, D = 5;
        for (int yy = 0; yy <= 4; yy++)
          for (int i = 0; i < W; i++)
            for (int j = 0; j < D; j++) {
              if (yy == 0) {
                place(x, y, z, i, yy, j, COBBLE, 0, ang, W, D);
                continue;
              }
              if (yy == 4) {
                place(x, y, z, i, yy, j, PLANKS, 0, ang, W, D);
                continue;
              }
              boolean corner = (i == 0 || i == W - 1) && (j == 0 || j == D - 1);
              boolean wall = (i == 0 || i == W - 1 || j == 0 || j == D - 1);
              if (corner) place(x, y, z, i, yy, j, WOOD, 0, ang, W, D);
              else if (wall) place(x, y, z, i, yy, j, PLANKS, 0, ang, W, D);
              else place(x, y, z, i, yy, j, AIR, 0, ang, W, D);
            }
        byte dData = getDoorData(ang, false);
        place(x, y, z, 2, 1, 0, WOOD_DOOR, dData, ang, W, D);
        place(x, y, z, 2, 2, 0, WOOD_DOOR, 8, ang, W, D);
        place(x, y, z, 2, 2, D - 1, GLASS_PANE, 0, ang, W, D);
        place(x, y, z, 0, 2, 2, GLASS_PANE, 0, ang, W, D);
        place(x, y, z, W - 1, 2, 2, GLASS_PANE, 0, ang, W, D);
        place(x, y, z, 2, 2, D - 2, TORCH, getTorchData(ang, 0), ang, W, D);
      }

      private void buildSmithy(int x, int y, int z, int ang) {
        int W = 10, D = 7;
        for (int i = 0; i < W; i++)
          for (int j = 0; j < D; j++) place(x, y, z, i, 0, j, COBBLE, 0, ang, W, D);
        for (int i = 0; i < W; i++)
          for (int j = 0; j < D; j++) place(x, y, z, i, 4, j, SLAB_COBBLE, 0, ang, W, D);
        for (int yy = 1; yy < 4; yy++) {
          for (int i = 0; i < W; i++) place(x, y, z, i, yy, D - 1, PLANKS, 0, ang, W, D);
          for (int j = 0; j < D; j++) place(x, y, z, W - 1, yy, j, PLANKS, 0, ang, W, D);
          place(x, y, z, 0, yy, 0, COBBLE, 0, ang, W, D);
          place(x, y, z, 0, yy, 1, COBBLE, 0, ang, W, D);
          place(x, y, z, 0, yy, 2, COBBLE, 0, ang, W, D);
        }
        for (int yy = 1; yy < 4; yy++) place(x, y, z, 5, yy, 0, COBBLE, 0, ang, W, D);
        place(x, y, z, 1, 1, 1, LAVA, 0, ang, W, D);
        place(x, y, z, 2, 1, 1, LAVA, 0, ang, W, D);
        place(x, y, z, 0, 1, 1, FURNACE, 5, ang, W, D);
        place(x, y, z, 0, 1, 2, FURNACE, 5, ang, W, D);
        place(x, y, z, 6, 1, 5, CHEST, 0, ang, W, D);
      }

      private void buildChurch(int x, int y, int z, int ang) {
        int W = 5, D = 9;
        for (int i = 0; i < W; i++)
          for (int j = 0; j < D; j++) place(x, y, z, i, 0, j, COBBLE, 0, ang, W, D);
        for (int yy = 1; yy < 10; yy++)
          for (int i = 0; i < W; i++)
            for (int j = 0; j < D; j++) {
              boolean wall = (i == 0 || i == W - 1 || j == 0 || j == D - 1);
              boolean tower = (j >= 5);
              if (yy > 4 && !tower) continue;
              if (wall) place(x, y, z, i, yy, j, COBBLE, 0, ang, W, D);
              else if (yy == 4 && !tower) place(x, y, z, i, yy, j, COBBLE, 0, ang, W, D);
              else place(x, y, z, i, yy, j, AIR, 0, ang, W, D);
            }
        byte dData = getDoorData(ang, false);
        place(x, y, z, 2, 1, 0, WOOD_DOOR, dData, ang, W, D);
        place(x, y, z, 2, 2, 0, WOOD_DOOR, 8, ang, W, D);
        place(x, y, z, 2, 3, 0, GLASS_PANE, 0, ang, W, D);
        place(x, y, z, 2, 7, D - 1, GLASS_PANE, 0, ang, W, D);
        place(x, y, z, 2, 6, D - 2, TORCH, getTorchData(ang, 0), ang, W, D);
      }

      private void buildLibrary(int x, int y, int z, int ang) {
        buildSmallHouse(x, y, z, ang);
        int W = 5, D = 5;
        place(x, y, z, 1, 1, 3, BOOKSHELF, 0, ang, W, D);
        place(x, y, z, 2, 1, 3, BOOKSHELF, 0, ang, W, D);
        place(x, y, z, 3, 1, 3, BOOKSHELF, 0, ang, W, D);
      }

      private void buildFarm(int x, int y, int z, int ang) {
        int W = 7, D = 9;
        for (int i = 0; i < W; i++)
          for (int j = 0; j < D; j++) {
            boolean border = (i == 0 || i == W - 1 || j == 0 || j == D - 1);
            if (border) place(x, y, z, i, 0, j, WOOD, 0, ang, W, D);
            else {
              boolean wet = (i == 3 && j > 0 && j < D - 1);
              if (wet) place(x, y, z, i, 0, j, WATER, 0, ang, W, D);
              else {
                place(x, y, z, i, 0, j, FARMLAND, 7, ang, W, D);
                place(x, y, z, i, 1, j, WHEAT_BLOCK, Math.abs(rnd.nextInt()) % 7, ang, W, D);
              }
            }
          }
      }

      private static final byte APPLE = (byte) 101;
    }
  }
}