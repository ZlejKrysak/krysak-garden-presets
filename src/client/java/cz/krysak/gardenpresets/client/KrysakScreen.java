package cz.krysak.gardenpresets.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class KrysakScreen extends Screen {

    // ---------------------------------------------------------------- palette
    private static final int BACKDROP = 0xC80B0D12, WINDOW = 0xFF11141C, SIDEBAR = 0xFF0D1016,
            SURFACE = 0xFF181C27, RAISED = 0xFF202535, RAISED_HOVER = 0xFF2A3045, FIELD = 0xFF0D1017,
            BORDER = 0xFF2A3040, ACCENT = 0xFF8870FF, ACCENT_LIGHT = 0xFFB8A7FF, ACCENT_DARK = 0xFF7059EC,
            TEXT = 0xFFF4F5FC, MUTED = 0xFF969DB1, SUCCESS = 0xFF59E39A, WARNING = 0xFFFFC766,
            ERROR = 0xFFFF657A, WHITE = 0xFFFFFFFF;

    // Mod logo = the same image as the mod icon (assets/krysak-garden-presets/icon.png)
    private static final Identifier LOGO = Identifier.fromNamespaceAndPath("krysak-garden-presets", "icon.png");
    private static final int LOGO_TEX = 128; // width AND height of icon.png in pixels

    // order = Crop enum order
    private static final int[] CROP_COLORS = {
            0xFF5CD66B, 0xFFFF9F43, 0xFFFF7A3D, 0xFFD9B86C, 0xFFB5773D,
            0xFFF2D14B, 0xFF7BE0A8, 0xFFE5566A, 0xFFC58BFF, 0xFF969DB1};
    private static final String[] CROP_SHORT = {
            "Melon", "Pumpkin", "Carrot", "Potato", "Cocoa", "Wheat", "Cane", "Wart", "Mushroom", "Custom"};
    // Minecraft items used as crop icons (same order as the Crop enum)
    private static final ItemStack[] CROP_ITEMS = {
            new ItemStack(Items.MELON_SLICE), new ItemStack(Items.PUMPKIN), new ItemStack(Items.CARROT),
            new ItemStack(Items.POTATO), new ItemStack(Items.COCOA_BEANS), new ItemStack(Items.WHEAT),
            new ItemStack(Items.SUGAR_CANE), new ItemStack(Items.NETHER_WART), new ItemStack(Items.RED_MUSHROOM),
            new ItemStack(Items.COMPASS)};

    // ---------------------------------------------------------------- 7x7 pixel icons
    private static final String[] I_SEARCH = {".###...", "#...#..", "#...#..", "#...#..", ".###...", ".....#.", "......#"};
    private static final String[] I_CLOSE = {"#.....#", ".#...#.", "..#.#..", "...#...", "..#.#..", ".#...#.", "#.....#"};
    private static final String[] I_CHECK = {".......", "......#", ".....#.", "#...#..", ".#.#...", "..#....", "......."};
    private static final String[] I_WARN = {"...#...", "..#.#..", "..#.#..", ".#.#.#.", ".#.#.#.", "#.....#", "#######"};
    private static final String[] I_GEAR = {"...#...", ".#.#.#.", "..###..", "###.###", "..###..", ".#.#.#.", "...#..."};
    private static final String[] I_PLAY = {".#.....", ".##....", ".###...", ".####..", ".###...", ".##....", ".#....."};
    private static final String[] I_EDIT = {".....##", "....###", "...###.", "..###..", ".###...", "##.#...", "##....."};
    private static final String[] I_TRASH = {"..###..", "#######", ".#.#.#.", ".#.#.#.", ".#.#.#.", ".#.#.#.", ".#####."};
    private static final String[] I_PLUS = {"...#...", "...#...", "...#...", "#######", "...#...", "...#...", "...#..."};
    private static final String[] I_MINUS = {".......", ".......", ".......", "#######", ".......", ".......", "......."};
    private static final String[] I_GRID = {"###.###", "###.###", "###.###", ".......", "###.###", "###.###", "###.###"};
    private static final String[] I_SPROUT = {"....##.", "...###.", ".#.##..", ".###...", "..#....", "..#....", "..#...."};
    private static final String[] I_CROSSHAIR = {"...#...", "...#...", ".......", "##...##", ".......", "...#...", "...#..."};

    private static final int CARD_H = 52, CARD_GAP = 8;
    private static final int STYLE_PRIMARY = 0, STYLE_SECONDARY = 1, STYLE_DANGER_SOFT = 2, STYLE_DANGER_SOLID = 3,
            STYLE_GHOST = 4;

    private enum Dialog {NONE, FORM, DELETE, SETTINGS}

    private record Hit(int x, int y, int w, int h, int clip0, int clip1, Runnable action) {
    }

    // ---------------------------------------------------------------- state
    private Dialog dialog = Dialog.NONE;
    private Dialog shown = Dialog.NONE; // dialog is still drawn while it fades out
    private Crop filter = null;
    private String editId = null;
    private Crop formCrop = Crop.MELON;
    private String formError = "";
    private boolean conflict = false;
    private Preset deleteTarget = null;
    private String selectedId = null;
    private String flashId = null;
    private long flashAt = 0;

    private final EditBox searchBox, nameBox, yawBox, pitchBox;

    // animation / frame state
    private final long openedAt = System.currentTimeMillis();
    private long lastFrame = 0;
    private float dt = 0.016f;
    private float alpha = 1f;
    private float dialogAnim = 0f;
    private float scroll = 0f, scrollTarget = 0f;
    private float durShown = -1f;
    private final Map<String, Float> hovers = new HashMap<>();
    private final List<Hit> hits = new ArrayList<>();
    private int mx, my, clipY0, clipY1;
    private boolean layerActive = true;
    private String toast = "";
    private int toastColor = SUCCESS;
    private long toastAt = 0;

    public KrysakScreen() {
        super(Component.literal("Krysak Garden Presets"));
        searchBox = newBox(40);
        nameBox = newBox(32);
        yawBox = newBox(10);
        pitchBox = newBox(10);
    }

    private static EditBox newBox(int maxLen) {
        EditBox b = new EditBox(Minecraft.getInstance().font, 0, 0, 100, 10, Component.empty());
        b.setBordered(false);
        b.setMaxLength(maxLen);
        b.setTextColor(TEXT);
        return b;
    }

    @Override
    protected void init() {
        addWidget(searchBox);
        addWidget(nameBox);
        addWidget(yawBox);
        addWidget(pitchBox);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final Pattern NUMBER = Pattern.compile("-?\\d{0,4}([.,]\\d{0,3})?");
    private final Map<EditBox, String> lastValid = new HashMap<>();

    /** Keeps only numbers like -90, 12.5 or -0,5 in a box; invalid input is reverted. */
    private void sanitize(EditBox box) {
        String v = box.getValue();
        if (NUMBER.matcher(v).matches()) {
            lastValid.put(box, v);
        } else {
            box.setValue(lastValid.getOrDefault(box, ""));
        }
    }

    // =====================================================================
    //  FRAME
    // =====================================================================

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        sanitize(yawBox);
        sanitize(pitchBox);

        long now = System.currentTimeMillis();
        dt = lastFrame == 0 ? 0.016f : Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        mx = mouseX;
        my = mouseY;
        hits.clear();
        clipY0 = 0;
        clipY1 = height;

        float open = Math.min(1f, (now - openedAt) / 220f);
        float baseAlpha = 1f - (1f - open) * (1f - open) * (1f - open);

        float target = dialog == Dialog.NONE ? 0f : 1f;
        dialogAnim += (target - dialogAnim) * (1f - (float) Math.exp(-dt * 16f));
        if (dialog == Dialog.NONE && dialogAnim < 0.02f) {
            dialogAnim = 0f;
            shown = Dialog.NONE;
        }

        alpha = baseAlpha;
        layerActive = dialog == Dialog.NONE;
        rect(g, 0, 0, width, height, BACKDROP);
        drawMain(g, delta, baseAlpha, now);

        if (shown != Dialog.NONE && dialogAnim > 0f) {
            alpha = baseAlpha * dialogAnim;
            layerActive = dialog != Dialog.NONE;
            rect(g, 0, 0, width, height, 0x99080A0E);
            drawDialog(g, delta);
        }
        alpha = baseAlpha;
    }

    // =====================================================================
    //  MAIN WINDOW
    // =====================================================================

    private void drawMain(GuiGraphicsExtractor g, float delta, float open, long now) {
        int W = Math.min(720, width - 24), H = Math.min(420, height - 24);
        int px = (width - W) / 2;
        int py = (height - H) / 2 + Math.round((1f - open) * 10f);
        final int HEADER = 46;
        int sideW = W >= 560 ? 168 : 128;
        Minecraft mc = Minecraft.getInstance();

        // ---- window shell ----
        shadow(g, px, py, W, H);
        round(g, px - 1, py - 1, W + 2, H + 2, BORDER);
        round(g, px, py, W, H, WINDOW);
        vGrad(g, px + 1, py + 2, W - 2, HEADER - 2, WINDOW, 0xFF151927);
        rail(g, px + 20, py + 1, W - 40, ACCENT_LIGHT, 0xA0);
        rect(g, px + 1, py + HEADER, W - 2, 1, BORDER);
        rect(g, px + 1, py + HEADER + 1, sideW, H - HEADER - 3, SIDEBAR);
        rect(g, px + sideW + 1, py + HEADER + 1, 1, H - HEADER - 3, BORDER);

        // ---- header: identity ----
        drawLogo(g, px + 16, py + 8);
        bold(g, "ZLEJKRYSAK", px + 56, py + 13, TEXT);
        spaced(g, "GARDEN PRESETS", px + 56, py + 26, MUTED);
        int titleEnd = px + 56 + spacedWidth("GARDEN PRESETS") + 18;

        // ---- header: controls (right to left) ----
        int closeX = px + W - 16 - 24;
        iconButton(g, "close", closeX, py + 11, 24, 24, I_CLOSE, ERROR, this::onClose);
        int r = closeX - 12;

        if (Rotator.isActive()) {
            fancyButton(g, "stop", r - 56, py + 11, 56, 24, "Stop", null, STYLE_DANGER_SOLID,
                    () -> Rotator.cancel("Rotation stopped."));
            r -= 64;
        }
        LocalPlayer player = mc.player;
        if (player != null) {
            String ys = fmt(Mth.wrapDegrees(player.getYRot()));
            String ps = fmt(player.getXRot());
            int wp = chipWidth("PITCH", ps) + 8, wy = chipWidth("YAW", ys) + 8; // statChip adds 8 px of padding
            if (r - wp - 8 - wy > titleEnd) {
                statChip(g, r - wp, py + 11, "PITCH", ps);
                r -= wp + 8;
                statChip(g, r - wy, py + 11, "YAW", ys);
            }
        }

        // ---- sidebar ----
        List<Preset> all = PresetStore.all();
        int sx = px + 10, sw = sideW - 18;
        spaced(g, "CATEGORIES", sx + 6, py + HEADER + 14, MUTED);
        int itemTop = py + HEADER + 30;
        int availH = (py + H - 10 - 28 - 10) - itemTop;
        int itemH = Mth.clamp(availH / 11 - 2, 18, 26);
        sideItem(g, "cat_all", sx, itemTop, sw, itemH, "All Presets", all.size(), filter == null, I_GRID, null,
                ACCENT_LIGHT, () -> {
                    filter = null;
                    scrollTarget = 0;
                });
        int i = 1;
        for (Crop c : Crop.values()) {
            int count = 0;
            for (Preset p : all) if (p.crop == c) count++;
            final Crop cc = c;
            sideItem(g, "cat_" + c.name(), sx, itemTop + i * (itemH + 2), sw, itemH, c.displayName, count,
                    filter == c, null, CROP_ITEMS[c.ordinal()], CROP_COLORS[c.ordinal()], () -> {
                        filter = cc;
                        scrollTarget = 0;
                    });
            i++;
        }
        fancyButton(g, "settings", sx, py + H - 10 - 28, sw, 28, "Settings", I_GEAR, STYLE_SECONDARY,
                () -> showDialog(Dialog.SETTINGS));

        // ---- controls row ----
        int x0 = px + sideW + 2 + 16, x1 = px + W - 16;
        int row1 = py + HEADER + 14;
        int newW = 128;
        fancyButton(g, "new", x1 - newW, row1, newW, 26, "New Preset", I_PLUS, STYLE_PRIMARY, () -> openForm(null));
        int searchW = x1 - newW - 10 - x0;
        boolean hasQuery = !searchBox.getValue().isEmpty();
        inputField(g, "search", searchBox, x0, row1, searchW, 26, "Search presets...", 26, hasQuery ? 30 : 12, delta);
        icon(g, I_SEARCH, x0 + 10, row1 + 9, 1, searchBox.isFocused() ? ACCENT_LIGHT : MUTED);
        if (hasQuery) {
            iconButton(g, "sclear", x0 + searchW - 22, row1 + 5, 16, 16, I_CLOSE, ERROR, () -> {
                searchBox.setValue("");
                scrollTarget = 0;
            });
        }

        // ---- list data ----
        String q = searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        List<Preset> view = new ArrayList<>();
        for (Preset p : all) {
            if (filter != null && p.crop != filter) continue;
            if (!q.isEmpty() && !p.name.toLowerCase(Locale.ROOT).contains(q)
                    && !p.crop.displayName.toLowerCase(Locale.ROOT).contains(q)) continue;
            view.add(p);
        }
        view.sort(Comparator.comparing((Preset p) -> p.name.toLowerCase(Locale.ROOT)));

        // ---- section title ----
        int ty = row1 + 26 + 16;
        String title = filter == null ? "All presets" : filter.displayName;
        bold(g, title, x0, ty, TEXT);
        String sub = view.size() + (view.size() == 1 ? " preset" : " presets") + (q.isEmpty() ? "" : " found");
        text(g, sub, x0 + font.width(title) + 10, ty, MUTED);

        int vy0 = ty + 9 + 10, vy1 = py + H - 12;
        int viewH = vy1 - vy0;
        int contentH = view.size() * (CARD_H + CARD_GAP);
        float maxScroll = Math.max(0, contentH - viewH);
        scrollTarget = Mth.clamp(scrollTarget, 0f, maxScroll);
        scroll += (scrollTarget - scroll) * (1f - (float) Math.exp(-dt * 14f));
        int off = Math.round(scroll);

        if (view.isEmpty()) {
            drawEmptyState(g, x0, x1, vy0, vy1, all.isEmpty(), q);
        }

        g.enableScissor(x0 - 3, vy0, x1 + 3, vy1);
        clipY0 = vy0;
        clipY1 = vy1;
        int cardW = x1 - x0 - (maxScroll > 0 ? 10 : 0);
        for (int k = 0; k < view.size(); k++) {
            int cy = vy0 + k * (CARD_H + CARD_GAP) - off;
            if (cy + CARD_H < vy0 || cy > vy1) continue;
            drawCard(g, view.get(k), x0, cy, cardW, now);
        }
        if (maxScroll > 0) {
            int barH = Math.max(20, (int) (viewH * (viewH / (float) contentH)));
            int barY = vy0 + (int) ((viewH - barH) * (scroll / maxScroll));
            round(g, x1 - 3, vy0, 3, viewH, 0xFF161A24);
            round(g, x1 - 3, barY, 3, barH, 0xFF3A4260);
        }
        g.disableScissor();
        clipY0 = 0;
        clipY1 = height;

        drawToast(g, px + sideW + 2, W - sideW - 2, py + H - 44, now);
    }

    /** Header logo: the mod icon image inside a softly glowing frame. */
    private void drawLogo(GuiGraphicsExtractor g, int x, int y) {
        final int S = 30;
        round(g, x - 3, y - 3, S + 6, S + 6, withAlpha(ACCENT, 0x1C));
        round(g, x - 2, y - 2, S + 4, S + 4, withAlpha(ACCENT, 0x2C));
        round(g, x - 1, y - 1, S + 2, S + 2, lerpColor(BORDER, ACCENT, 0.6f));
        round(g, x, y, S, S, FIELD);
        if (alpha >= 0.6f) { // images cannot fade, so draw once the window is mostly visible
            g.blit(RenderPipelines.GUI_TEXTURED, LOGO, x + 2, y + 2, 0, 0, S - 4, S - 4,
                    LOGO_TEX, LOGO_TEX, LOGO_TEX, LOGO_TEX);
        }
    }

    private void drawEmptyState(GuiGraphicsExtractor g, int x0, int x1, int vy0, int vy1, boolean firstRun, String q) {
        int cx = (x0 + x1) / 2;
        int cy = (vy0 + vy1) / 2 - 8;
        String[] ic = !q.isEmpty() ? I_SEARCH : I_SPROUT;
        String head, body, btn;
        Runnable act;
        if (!q.isEmpty()) {
            head = "No results";
            body = "Nothing matches your search.";
            btn = "Clear search";
            act = () -> searchBox.setValue("");
        } else if (firstRun) {
            head = "No presets yet";
            body = "Save a camera angle and reuse it with one click.";
            btn = "Create preset";
            act = () -> openForm(null);
        } else {
            head = "Nothing in " + (filter == null ? "this view" : filter.displayName);
            body = "Create a preset for this crop.";
            btn = "Create preset";
            act = () -> openForm(null);
        }
        round(g, cx - 21, cy - 58, 42, 42, BORDER);
        round(g, cx - 20, cy - 57, 40, 40, SURFACE);
        icon(g, ic, cx - 10, cy - 47, 3, ACCENT_LIGHT);
        textCentered(g, head, cx, cy - 8, TEXT);
        textCentered(g, trim(body, x1 - x0 - 20), cx, cy + 6, MUTED);
        fancyButton(g, "empty_btn", cx - 62, cy + 24, 124, 26, btn,
                !q.isEmpty() ? null : I_PLUS, !q.isEmpty() ? STYLE_SECONDARY : STYLE_PRIMARY, act);
    }

    private void drawCard(GuiGraphicsExtractor g, Preset p, int x, int y, int w, long now) {
        float hv = hov("card_" + p.id, inside(x, y, w, CARD_H));
        float sv = hov("cardsel_" + p.id, p.id.equals(selectedId));
        float fl = p.id.equals(flashId) ? Math.max(0f, 1f - (now - flashAt) / 900f) : 0f;
        int col = CROP_COLORS[p.crop.ordinal()];

        if (fl > 0f) round(g, x - 2, y - 2, w + 4, CARD_H + 4, withAlpha(SUCCESS, (int) (80 * fl)));
        int border = lerpColor(lerpColor(BORDER, 0xFF4A4370, hv), ACCENT, sv);
        int bg = lerpColor(lerpColor(SURFACE, 0xFF1D2231, hv), 0xFF211F37, sv);
        round(g, x, y, w, CARD_H, border);
        round(g, x + 1, y + 1, w - 2, CARD_H - 2, bg);
        if (layerActive) hits.add(new Hit(x, y, w, CARD_H, clipY0, clipY1, () -> selectedId = p.id));

        // crop tile with the Minecraft item
        int tx = x + 12, ty = y + (CARD_H - 34) / 2;
        round(g, tx, ty, 34, 34, lerpColor(bg, col, 0.35f));
        round(g, tx + 1, ty + 1, 32, 32, lerpColor(bg, col, 0.12f + 0.06f * hv));
        cropItem(g, p.crop, tx + 9, ty + 9);

        // buttons (fixed size, independent of name length)
        boolean compact = w < 430;
        int b1 = compact ? 50 : 66, b2 = compact ? 44 : 60, b3 = compact ? 56 : 72, gap = 6;
        int bx = x + w - 12 - (b1 + b2 + b3 + gap * 2);
        int by = y + (CARD_H - 26) / 2;
        fancyButton(g, "load_" + p.id, bx, by, b1, 26, "Load", compact ? null : I_PLAY, STYLE_PRIMARY,
                () -> loadPreset(p));
        fancyButton(g, "edit_" + p.id, bx + b1 + gap, by, b2, 26, "Edit", compact ? null : I_EDIT, STYLE_SECONDARY,
                () -> openForm(p));
        fancyButton(g, "del_" + p.id, bx + b1 + gap + b2 + gap, by, b3, 26, "Delete", compact ? null : I_TRASH,
                STYLE_DANGER_SOFT, () -> openDelete(p));

        // texts
        int textX = x + 58;
        int avail = bx - textX - 12;
        bold(g, trim(p.name, avail), textX, y + 10, TEXT);
        int cxp = textX;
        int cy = y + 27;
        cxp += tag(g, cxp, cy, p.crop.displayName, col, avail - (cxp - textX)) + 6;
        String ys = fmt(p.yaw), ps = fmt(p.pitch);
        int wy = chipWidth("YAW", ys), wp = chipWidth("PITCH", ps);
        if (cxp - textX + wy <= avail) {
            miniChip(g, cxp, cy, "YAW", ys);
            cxp += wy + 6;
            if (cxp - textX + wp <= avail) miniChip(g, cxp, cy, "PITCH", ps);
        }
    }

    /** Draws the vanilla item icon of a crop (16x16). Items cannot fade, so they appear once the window is mostly visible. */
    private void cropItem(GuiGraphicsExtractor g, Crop c, int x, int y) {
        if (alpha < 0.6f) return;
        g.item(CROP_ITEMS[c.ordinal()], x, y);
    }

    private int tag(GuiGraphicsExtractor g, int x, int y, String label, int col, int maxW) {
        String t = trim(label, Math.max(20, maxW - 12));
        int w = font.width(t) + 12;
        round(g, x, y, w, 14, lerpColor(SURFACE, col, 0.22f));
        text(g, t, x + 6, y + 3, col);
        return w;
    }

    private void miniChip(GuiGraphicsExtractor g, int x, int y, String label, String value) {
        int w = chipWidth(label, value);
        round(g, x, y, w, 14, 0xFF1F2433);
        text(g, label, x + 6, y + 3, MUTED);
        text(g, value, x + 6 + font.width(label) + 5, y + 3, TEXT);
    }

    private int chipWidth(String label, String value) {
        return 12 + font.width(label) + 5 + font.width(value);
    }

    private void statChip(GuiGraphicsExtractor g, int x, int y, String label, String value) {
        int w = chipWidth(label, value) + 8;
        round(g, x, y, w, 24, BORDER);
        round(g, x + 1, y + 1, w - 2, 22, SURFACE);
        text(g, label, x + 10, y + 8, MUTED);
        text(g, value, x + 10 + font.width(label) + 5, y + 8, TEXT);
    }

    private void sideItem(GuiGraphicsExtractor g, String id, int x, int y, int w, int h, String label, int count,
                          boolean selected, String[] ic, ItemStack item, int color, Runnable action) {
        float hv = hov(id, inside(x, y, w, h));
        float sv = hov(id + "_s", selected);
        float lit = Math.max(hv * 0.8f, sv);
        int bg = lerpColor(lerpColor(0x00202535, 0x90202535, hv), withAlpha(ACCENT, 0x30), sv);
        round(g, x, y, w, h, bg);
        if (sv > 0.01f) rect(g, x, y + 5, 2, h - 10, withAlpha(ACCENT_LIGHT, (int) (255 * sv)));
        if (item != null) {
            if (alpha > 0.6f) g.item(item, x + 7, y + (h - 16) / 2);
        } else {
            icon(g, ic, x + 11, y + (h - 7) / 2, 1, lerpColor(withAlpha(color, 0xA0), color, lit));
        }
        String cnt = String.valueOf(count);
        int pw = Math.max(18, font.width(cnt) + 10);
        text(g, trim(label, w - 29 - pw - 14), x + 29, y + (h - 8) / 2, lerpColor(MUTED, TEXT, lit));
        int px2 = x + w - 8 - pw, py2 = y + (h - 13) / 2;
        round(g, px2, py2, pw, 13, lerpColor(0xFF1A1E2A, 0xFF3A3170, sv));
        text(g, cnt, px2 + (pw - font.width(cnt)) / 2, py2 + 3, lerpColor(MUTED, ACCENT_LIGHT, sv));
        if (layerActive) hits.add(new Hit(x, y, w, h, clipY0, clipY1, action));
    }

    private void drawToast(GuiGraphicsExtractor g, int areaX, int areaW, int baseY, long now) {
        long age = now - toastAt;
        if (toast.isEmpty() || age >= 2800) return;
        float saved = alpha;
        float in = Math.min(1f, age / 200f);
        alpha *= Math.min(in, Math.min(1f, (2800 - age) / 400f));
        int tw = font.width(toast) + 40;
        int tx = areaX + (areaW - tw) / 2;
        int ty = baseY + Math.round((1f - in) * 8f);
        round(g, tx - 2, ty - 2, tw + 4, 30, withAlpha(toastColor, 0x30));
        round(g, tx, ty, tw, 26, lerpColor(RAISED, toastColor, 0.5f));
        round(g, tx + 1, ty + 1, tw - 2, 24, RAISED);
        icon(g, toastColor == SUCCESS ? I_CHECK : I_WARN, tx + 11, ty + 9, 1, toastColor);
        text(g, toast, tx + 27, ty + 9, TEXT);
        alpha = saved;
    }

    // =====================================================================
    //  DIALOGS
    // =====================================================================

    private void drawDialog(GuiGraphicsExtractor g, float delta) {
        int dw = switch (shown) {
            case SETTINGS -> 340;
            default -> 320;
        };
        int dh = switch (shown) {
            case FORM -> 316;
            case DELETE -> 150;
            case SETTINGS -> 170;
            default -> 100;
        };
        int dx = (width - dw) / 2;
        int dy = (height - dh) / 2 + Math.round((1f - dialogAnim) * 10f);

        String title = switch (shown) {
            case FORM -> editId == null ? "New preset" : "Edit preset";
            case DELETE -> "Delete preset";
            case SETTINGS -> "Settings";
            default -> "";
        };
        shadow(g, dx, dy, dw, dh);
        round(g, dx - 1, dy - 1, dw + 2, dh + 2, BORDER);
        round(g, dx, dy, dw, dh, WINDOW);
        rail(g, dx + 16, dy + 1, dw - 32, ACCENT_LIGHT, 0x90);
        bold(g, title, dx + 18, dy + 17, TEXT);
        iconButton(g, "dlg_close", dx + dw - 18 - 22, dy + 10, 22, 22, I_CLOSE, ERROR, this::closeDialog);
        rect(g, dx + 1, dy + 42, dw - 2, 1, BORDER);

        switch (shown) {
            case FORM -> drawForm(g, dx, dy, dw, dh, delta);
            case DELETE -> drawDelete(g, dx, dy, dw, dh);
            case SETTINGS -> drawSettings(g, dx, dy, dw, dh);
            default -> {
            }
        }
    }

    private void drawForm(GuiGraphicsExtractor g, int dx, int dy, int dw, int dh, float delta) {
        int ix = dx + 18, iw = dw - 36;

        spaced(g, "NAME", ix, dy + 54, MUTED);
        inputField(g, "f_name", nameBox, ix, dy + 66, iw, 24, "e.g. Melon Left", 10, 10, delta);

        spaced(g, "CROP", ix, dy + 100, MUTED);
        text(g, formCrop.displayName, ix + spacedWidth("CROP") + 10, dy + 100, CROP_COLORS[formCrop.ordinal()]);
        Crop[] crops = Crop.values();
        for (int i = 0; i < crops.length; i++) {
            final Crop c = crops[i];
            int cx = ix + (i % 5) * 58, cy = dy + 112 + (i / 5) * 32;
            float hv = hov("tile_" + i, inside(cx, cy, 52, 26));
            float sv = hov("tile_s_" + i, formCrop == c);
            int col = CROP_COLORS[i];
            round(g, cx, cy, 52, 26, lerpColor(lerpColor(BORDER, 0xFF3A4260, hv), col, sv));
            round(g, cx + 1, cy + 1, 50, 24, lerpColor(lerpColor(SURFACE, RAISED, hv), lerpColor(SURFACE, col, 0.2f), sv));
            cropItem(g, c, cx + 18, cy + 5);
            if (layerActive) hits.add(new Hit(cx, cy, 52, 26, clipY0, clipY1, () -> formCrop = c));
        }

        int fw = (iw - 8) / 2;
        spaced(g, "YAW", ix, dy + 180, MUTED);
        spaced(g, "PITCH", ix + fw + 8, dy + 180, MUTED);
        inputField(g, "f_yaw", yawBox, ix, dy + 192, fw, 24, "0.0", 10, 24, delta);
        inputField(g, "f_pitch", pitchBox, ix + fw + 8, dy + 192, fw, 24, "0.0", 10, 24, delta);
        text(g, "\u00B0", ix + fw - 14, dy + 200, MUTED);
        text(g, "\u00B0", ix + fw + 8 + fw - 14, dy + 200, MUTED);

        fancyButton(g, "usecur", ix, dy + 224, iw, 24, "Use my current view", I_CROSSHAIR, STYLE_GHOST,
                this::useCurrentView);

        if (!formError.isEmpty()) {
            int ec = conflict ? WARNING : ERROR;
            icon(g, I_WARN, ix, dy + 256, 1, ec);
            text(g, trim(formError, iw - 14), ix + 14, dy + 256, ec);
        }

        int by = dy + dh - 18 - 26;
        if (conflict) {
            fancyButton(g, "rename", ix, by, 120, 26, "Change name", null, STYLE_SECONDARY, () -> {
                conflict = false;
                formError = "";
                setFocused(nameBox);
            });
            fancyButton(g, "overwrite", dx + dw - 18 - 130, by, 130, 26, "Overwrite", null, STYLE_DANGER_SOLID,
                    () -> trySave(true));
        } else {
            fancyButton(g, "cancel", ix, by, 110, 26, "Cancel", null, STYLE_SECONDARY, this::closeDialog);
            fancyButton(g, "save", dx + dw - 18 - 130, by, 130, 26, "Save preset", I_CHECK, STYLE_PRIMARY,
                    () -> trySave(false));
        }
    }

    private void drawDelete(GuiGraphicsExtractor g, int dx, int dy, int dw, int dh) {
        int tx = dx + 18, ty = dy + 58;
        round(g, tx, ty, 36, 36, lerpColor(WINDOW, ERROR, 0.4f));
        round(g, tx + 1, ty + 1, 34, 34, lerpColor(WINDOW, ERROR, 0.16f));
        icon(g, I_WARN, tx + 11, ty + 11, 2, ERROR);
        String name = deleteTarget == null ? "" : deleteTarget.name;
        bold(g, trim(name, dw - 86), dx + 66, ty + 6, TEXT);
        text(g, "This will permanently remove the preset.", dx + 66, ty + 21, MUTED);

        int by = dy + dh - 18 - 26;
        fancyButton(g, "d_cancel", dx + 18, by, 110, 26, "Cancel", null, STYLE_SECONDARY, this::closeDialog);
        fancyButton(g, "d_delete", dx + dw - 18 - 130, by, 130, 26, "Delete", I_TRASH, STYLE_DANGER_SOLID,
                this::confirmDelete);
    }

    private void drawSettings(GuiGraphicsExtractor g, int dx, int dy, int dw, int dh) {
        int ix = dx + 18, iw = dw - 36;
        if (durShown < 0) durShown = Settings.durationMs;
        durShown += (Settings.durationMs - durShown) * (1f - (float) Math.exp(-dt * 12f));

        // label + value on one line, description below (nothing overlaps)
        text(g, "Rotation duration", ix, dy + 56, TEXT);
        String val = Settings.durationMs + " ms";
        text(g, val, ix + iw - font.width(val), dy + 56, ACCENT_LIGHT);
        text(g, "How long the camera takes to reach a preset.", ix, dy + 70, MUTED);

        // control row: [-]  ====o-----  [+]
        int ry = dy + 92;
        iconButton(g, "dur_minus", ix, ry, 24, 24, I_MINUS, ACCENT, () -> {
            Settings.durationMs = Math.max(Settings.MIN_DURATION_MS, Settings.durationMs - 100);
            Settings.save();
        });
        iconButton(g, "dur_plus", ix + iw - 24, ry, 24, 24, I_PLUS, ACCENT, () -> {
            Settings.durationMs = Math.min(Settings.MAX_DURATION_MS, Settings.durationMs + 100);
            Settings.save();
        });
        int tx = ix + 34, tw = iw - 68;
        float frac = (durShown - Settings.MIN_DURATION_MS) / (Settings.MAX_DURATION_MS - Settings.MIN_DURATION_MS);
        round(g, tx, ry + 9, tw, 6, FIELD);
        int fillW = Math.max(6, Math.round(tw * Mth.clamp(frac, 0f, 1f)));
        roundGrad(g, tx, ry + 9, fillW, 6, ACCENT_LIGHT, ACCENT_DARK);
        int knobX = Mth.clamp(tx + fillW - 4, tx, tx + tw - 8);
        round(g, knobX - 1, ry + 6, 10, 12, withAlpha(ACCENT, 0x55));
        round(g, knobX, ry + 7, 8, 10, WHITE);

        fancyButton(g, "s_close", dx + dw - 18 - 110, dy + dh - 18 - 26, 110, 26, "Done", I_CHECK, STYLE_PRIMARY,
                this::closeDialog);
    }

    // =====================================================================
    //  ACTIONS
    // =====================================================================

    private void showDialog(Dialog d) {
        dialog = d;
        shown = d;
        setFocused(null);
    }

    private void closeDialog() {
        dialog = Dialog.NONE;
        setFocused(null);
    }

    private void openForm(Preset p) {
        editId = p == null ? null : p.id;
        nameBox.setValue(p == null ? "" : p.name);
        formCrop = p != null ? p.crop : (filter != null ? filter : Crop.MELON);
        if (p != null) {
            yawBox.setValue(fmt(p.yaw));
            pitchBox.setValue(fmt(p.pitch));
        } else {
            useCurrentView();
        }
        formError = "";
        conflict = false;
        showDialog(Dialog.FORM);
        setFocused(nameBox);
    }

    private void useCurrentView() {
        LocalPlayer pl = Minecraft.getInstance().player;
        if (pl == null) return;
        yawBox.setValue(fmt(Mth.wrapDegrees(pl.getYRot())));
        pitchBox.setValue(fmt(pl.getXRot()));
    }

    private void trySave(boolean overwrite) {
        String name = nameBox.getValue().trim();
        if (Preset.keyOf(name).isEmpty()) {
            formError = "Enter a name (letters or digits).";
            conflict = false;
            return;
        }
        Float yaw = parse(yawBox.getValue());
        Float pitch = parse(pitchBox.getValue());
        if (yaw == null || pitch == null) {
            formError = "Yaw and Pitch must be numbers.";
            conflict = false;
            return;
        }
        if (!PresetStore.upsert(editId, name, formCrop, yaw, pitch, overwrite)) {
            Preset other = PresetStore.conflict(name, editId);
            conflict = true;
            formError = "'" + (other != null ? other.name : name) + "' already exists.";
            return;
        }
        Preset saved = PresetStore.findByKey(name);
        if (saved != null) {
            selectedId = saved.id;
            flashId = saved.id;
            flashAt = System.currentTimeMillis();
        }
        closeDialog();
        showToast("Saved '" + name + "'", SUCCESS);
    }

    private void openDelete(Preset p) {
        deleteTarget = p;
        showDialog(Dialog.DELETE);
    }

    private void confirmDelete() {
        if (deleteTarget != null && PresetStore.deleteById(deleteTarget.id)) {
            if (deleteTarget.id.equals(selectedId)) selectedId = null;
            showToast("Deleted '" + deleteTarget.name + "'", SUCCESS);
        }
        deleteTarget = null;
        closeDialog();
    }

    private void loadPreset(Preset p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        selectedId = p.id;
        onClose();
        Rotator.start(mc.player, p, Settings.durationMs);
    }

    private void showToast(String s, int color) {
        toast = s;
        toastColor = color;
        toastAt = System.currentTimeMillis();
    }

    // =====================================================================
    //  INPUT
    // =====================================================================

    private List<EditBox> activeBoxes() {
        return switch (dialog) {
            case NONE -> List.of(searchBox);
            case FORM -> List.of(nameBox, yawBox, pitchBox);
            default -> List.of();
        };
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return true;
        for (EditBox b : activeBoxes()) {
            if (b.mouseClicked(event, doubleClick)) {
                setFocused(b);
                return true;
            }
        }
        double ex = event.x(), ey = event.y();
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (ex >= h.x() && ex < h.x() + h.w() && ey >= h.y() && ey < h.y() + h.h()
                    && ey >= h.clip0() && ey < h.clip1()) {
                setFocused(null);
                h.action().run();
                return true;
            }
        }
        setFocused(null);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (dialog == Dialog.NONE) scrollTarget -= (float) scrollY * 40f;
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == 256) { // ESC
            if (dialog != Dialog.NONE) closeDialog();
            else onClose();
            return true;
        }
        if (key == 257 || key == 335) { // ENTER
            if (dialog == Dialog.FORM) {
                trySave(conflict);
                return true;
            }
            if (dialog == Dialog.DELETE) {
                confirmDelete();
                return true;
            }
        }
        return super.keyPressed(event);
    }

    // =====================================================================
    //  DRAWING PRIMITIVES
    // =====================================================================

    private int a(int argb) {
        int al = Mth.clamp((int) (((argb >>> 24) & 255) * alpha), 0, 255);
        return (al << 24) | (argb & 0xFFFFFF);
    }

    private static int withAlpha(int rgb, int al) {
        return (Mth.clamp(al, 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    private static int lerpColor(int c1, int c2, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int out = 0;
        for (int shift = 24; shift >= 0; shift -= 8) {
            int v1 = (c1 >>> shift) & 255, v2 = (c2 >>> shift) & 255;
            out |= ((int) (v1 + (v2 - v1) * t) & 255) << shift;
        }
        return out;
    }

    private void rect(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + h, a(color));
    }

    /** Rectangle with softly cut corners. Rows do not overlap, so translucent colors stay clean. */
    private void round(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        if (w < 5 || h < 5) {
            g.fill(x, y, x + w, y + h, a(color));
            return;
        }
        int c = a(color);
        g.fill(x + 2, y, x + w - 2, y + 1, c);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, c);
        g.fill(x, y + 2, x + w, y + h - 2, c);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, c);
        g.fill(x + 2, y + h - 1, x + w - 2, y + h, c);
    }

    private void roundGrad(GuiGraphicsExtractor g, int x, int y, int w, int h, int top, int bottom) {
        for (int i = 0; i < h; i++) {
            int inset = (i == 0 || i == h - 1) ? 2 : (i == 1 || i == h - 2) ? 1 : 0;
            int c = a(lerpColor(top, bottom, h <= 1 ? 0f : i / (float) (h - 1)));
            g.fill(x + inset, y + i, x + w - inset, y + i + 1, c);
        }
    }

    private void vGrad(GuiGraphicsExtractor g, int x, int y, int w, int h, int top, int bottom) {
        for (int i = 0; i < h; i++) {
            g.fill(x, y + i, x + w, y + i + 1, a(lerpColor(top, bottom, h <= 1 ? 0f : i / (float) (h - 1))));
        }
    }

    /** Thin horizontal light that fades out toward both ends. */
    private void rail(GuiGraphicsExtractor g, int x, int y, int w, int color, int maxAlpha) {
        int seg = 32;
        for (int i = 0; i < seg; i++) {
            float bell = (float) Math.sin(Math.PI * (i + 0.5f) / seg);
            int x1 = x + w * i / seg, x2 = x + w * (i + 1) / seg;
            g.fill(x1, y, x2, y + 1, a(withAlpha(color, (int) (maxAlpha * bell * bell))));
        }
    }

    private void shadow(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        for (int i = 10; i >= 1; i--) round(g, x - i, y - i + 4, w + 2 * i, h + 2 * i, 0x0A000000);
    }

    private void icon(GuiGraphicsExtractor g, String[] bmp, int x, int y, int s, int color) {
        int c = a(color);
        for (int r = 0; r < bmp.length; r++) {
            String row = bmp[r];
            int start = -1;
            for (int col = 0; col <= row.length(); col++) {
                boolean on = col < row.length() && row.charAt(col) == '#';
                if (on && start < 0) start = col;
                if (!on && start >= 0) {
                    g.fill(x + start * s, y + r * s, x + col * s, y + (r + 1) * s, c);
                    start = -1;
                }
            }
        }
    }

    private void text(GuiGraphicsExtractor g, String s, int x, int y, int color) {
        int c = a(color);
        if ((c >>> 24) < 10) return; // near-transparent text would render opaque
        g.text(font, s, x, y, c, false);
    }

    private void bold(GuiGraphicsExtractor g, String s, int x, int y, int color) {
        text(g, s, x, y, color);
        text(g, s, x + 1, y, color);
    }

    private void textCentered(GuiGraphicsExtractor g, String s, int cx, int y, int color) {
        text(g, s, cx - font.width(s) / 2, y, color);
    }

    private int spacedWidth(String s) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) w += font.width(String.valueOf(s.charAt(i))) + 1;
        return w;
    }

    private void spaced(GuiGraphicsExtractor g, String s, int x, int y, int color) {
        int cx = x;
        for (int i = 0; i < s.length(); i++) {
            String t = String.valueOf(s.charAt(i));
            text(g, t, cx, y, color);
            cx += font.width(t) + 1;
        }
    }

    private String trim(String s, int maxW) {
        if (font.width(s) <= maxW) return s;
        String t = s;
        while (t.length() > 1 && font.width(t + "..") > maxW) t = t.substring(0, t.length() - 1);
        return t + "..";
    }

    // =====================================================================
    //  COMPONENTS
    // =====================================================================

    private boolean inside(int x, int y, int w, int h) {
        return layerActive && mx >= x && mx < x + w && my >= y && my < y + h && my >= clipY0 && my < clipY1;
    }

    private float hov(String id, boolean target) {
        float v = hovers.getOrDefault(id, 0f);
        v += ((target ? 1f : 0f) - v) * (1f - (float) Math.exp(-dt * 18f));
        hovers.put(id, v);
        return v;
    }

    private void fancyButton(GuiGraphicsExtractor g, String id, int x, int y, int w, int h, String label,
                             String[] ic, int style, Runnable action) {
        float hv = hov(id, inside(x, y, w, h));
        int fg;
        switch (style) {
            case STYLE_PRIMARY -> {
                if (hv > 0.01f) round(g, x - 2, y - 2, w + 4, h + 4, withAlpha(ACCENT, (int) (0x34 * hv)));
                roundGrad(g, x, y, w, h, lerpColor(0xFF9A85FF, 0xFFAD9BFF, hv), lerpColor(ACCENT_DARK, ACCENT, hv));
                rect(g, x + 3, y + 1, w - 6, 1, withAlpha(WHITE, 0x3C));
                fg = WHITE;
            }
            case STYLE_DANGER_SOLID -> {
                if (hv > 0.01f) round(g, x - 2, y - 2, w + 4, h + 4, withAlpha(ERROR, (int) (0x34 * hv)));
                roundGrad(g, x, y, w, h, lerpColor(0xFFFF7D8F, 0xFFFF93A2, hv), lerpColor(0xFFE04A63, ERROR, hv));
                rect(g, x + 3, y + 1, w - 6, 1, withAlpha(WHITE, 0x3C));
                fg = WHITE;
            }
            case STYLE_DANGER_SOFT -> {
                round(g, x, y, w, h, lerpColor(0xFF3C3448, ERROR, hv));
                round(g, x + 1, y + 1, w - 2, h - 2, lerpColor(RAISED, 0xFFE0506A, hv));
                fg = lerpColor(lerpColor(MUTED, ERROR, 0.75f), WHITE, hv);
            }
            case STYLE_GHOST -> {
                round(g, x, y, w, h, lerpColor(BORDER, ACCENT, hv * 0.6f));
                round(g, x + 1, y + 1, w - 2, h - 2, lerpColor(SURFACE, RAISED, hv));
                fg = lerpColor(MUTED, TEXT, hv);
            }
            default -> {
                round(g, x, y, w, h, lerpColor(BORDER, 0xFF3A4260, hv));
                round(g, x + 1, y + 1, w - 2, h - 2, lerpColor(RAISED, RAISED_HOVER, hv));
                fg = TEXT;
            }
        }
        int total = font.width(label) + (ic != null ? 12 : 0);
        int sx = x + (w - total) / 2;
        if (ic != null) {
            icon(g, ic, sx, y + (h - 7) / 2, 1, fg);
            sx += 12;
        }
        text(g, label, sx, y + (h - 8) / 2, fg);
        if (layerActive) hits.add(new Hit(x, y, w, h, clipY0, clipY1, action));
    }

    private void iconButton(GuiGraphicsExtractor g, String id, int x, int y, int w, int h, String[] ic,
                            int hoverTint, Runnable action) {
        float hv = hov(id, inside(x, y, w, h));
        round(g, x, y, w, h, lerpColor(BORDER, hoverTint, hv * 0.8f));
        round(g, x + 1, y + 1, w - 2, h - 2, lerpColor(SURFACE, lerpColor(RAISED, hoverTint, 0.28f), hv));
        icon(g, ic, x + (w - 7) / 2, y + (h - 7) / 2, 1, lerpColor(MUTED, WHITE, hv));
        if (layerActive) hits.add(new Hit(x, y, w, h, clipY0, clipY1, action));
    }

    private void inputField(GuiGraphicsExtractor g, String id, EditBox box, int x, int y, int w, int h,
                            String placeholder, int padL, int padR, float delta) {
        float fv = hov(id + "_focus", box.isFocused());
        if (fv > 0.01f) round(g, x - 2, y - 2, w + 4, h + 4, withAlpha(ACCENT, (int) (0x30 * fv)));
        round(g, x, y, w, h, lerpColor(BORDER, ACCENT, fv));
        round(g, x + 1, y + 1, w - 2, h - 2, FIELD);
        box.setPosition(x + padL, y + (h - 8) / 2);
        box.setWidth(Math.max(10, w - padL - padR));
        box.setAlpha(alpha);
        box.extractRenderState(g, mx, my, delta);
        if (box.getValue().isEmpty() && !box.isFocused()) text(g, placeholder, x + padL, y + (h - 8) / 2, MUTED);
        if (layerActive) hits.add(new Hit(x, y, w, h, clipY0, clipY1, () -> setFocused(box)));
    }

    private static String fmt(float v) {
        if (Math.abs(v) < 0.05f) v = 0f;
        return String.format(Locale.ROOT, "%.1f", v);
    }

    private static Float parse(String s) {
        try {
            return Float.parseFloat(s.trim().replace(',', '.'));
        } catch (Exception e) {
            return null;
        }
    }
}