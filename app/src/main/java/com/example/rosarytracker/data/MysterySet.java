package com.example.rosarytracker.data;

import com.example.rosarytracker.R;
import java.util.Calendar;

/**
 * Represents the four sets of Rosary mysteries with 20 unique images, descriptions, and timestamps.
 */
public enum MysterySet {

    JOYFUL("Joyful", new String[]{
            "The Annunciation", "The Visitation", "The Nativity",
            "The Presentation in the Temple", "The Finding in the Temple"
    }, new String[]{
            "The Archangel Gabriel announces to Mary that she will conceive the Son of God.",
            "Mary visits her cousin Elizabeth, who recognizes Mary as the Mother of the Lord.",
            "Jesus is born in a stable in Bethlehem, the Word made flesh.",
            "Mary and Joseph present the infant Jesus in the Temple according to the Law.",
            "After three days, Mary and Joseph find the young Jesus teaching in the Temple."
    }, 
       new String[]{
            "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4b/La_Anunciaci%C3%B3n%2C_by_Fra_Angelico%2C_from_Prado_in_Google_Earth.jpg/1280px-La_Anunciaci%C3%B3n%2C_by_Fra_Angelico%2C_from_Prado_in_Google_Earth.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e0/Pontormo%2C_Visitation%2C_1516%2C_SS_Annunziata%2C_Chiostrino_dei_Voti%2C_Florence.jpg/1280px-Pontormo%2C_Visitation%2C_1516%2C_SS_Annunziata%2C_Chiostrino_dei_Voti%2C_Florence.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/Giorgione_-_Adoration_of_the_Shepherds_-_National_Gallery_of_Art.jpg/1280px-Giorgione_-_Adoration_of_the_Shepherds_-_National_Gallery_of_Art.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/7/77/Fra_Angelico%2C_Presentation_in_the_Temple%2C_ca._1436-45%3B_Convent_of_San_Marco%2C_Florence_%282%29.jpg/1280px-Fra_Angelico%2C_Presentation_in_the_Temple%2C_ca._1436-45%3B_Convent_of_San_Marco%2C_Florence_%282%29.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f0/William_Holman_Hunt_-_The_Finding_of_the_Saviour_in_the_Temple_-_Google_Art_Project.jpg/1280px-William_Holman_Hunt_-_The_Finding_of_the_Saviour_in_the_Temple_-_Google_Art_Project.jpg"
       }),
    
    LUMINOUS("Luminous", new String[]{
            "The Baptism of Christ", "The Wedding Feast at Cana", "Proclamation of the Kingdom",
            "The Transfiguration", "Institution of the Eucharist"
    }, new String[]{
            "Jesus is baptized by John, and the Father declares: 'This is my beloved Son.'",
            "Jesus performs his first miracle, turning water into wine at Mary's request.",
            "Jesus calls all to conversion and announces the arrival of the Kingdom of God.",
            "Jesus reveals his divine glory to Peter, James, and John on Mount Tabor.",
            "Jesus gives us his Body and Blood as spiritual food at the Last Supper."
    },
       new String[]{
            "https://upload.wikimedia.org/wikipedia/commons/thumb/9/92/Piero_della_Francesca_-_Battesimo_di_Cristo_%28National_Gallery%2C_London%29.jpg/1280px-Piero_della_Francesca_-_Battesimo_di_Cristo_%28National_Gallery%2C_London%29.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cb/Les_Noces_de_Cana_-_Paolo_Veronese_-_Mus%C3%A9e_du_Louvre_Peintures_INV_142_%3B_MR_384.jpg/1280px-Les_Noces_de_Cana_-_Paolo_Veronese_-_Mus%C3%A9e_du_Louvre_Peintures_INV_142_%3B_MR_384.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/9/96/Bloch-SermonOnTheMount.jpg/1280px-Bloch-SermonOnTheMount.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e3/Raphael%2C_Transfiguration%2C_1518-20%3B_Vatican_Museums_%282%29_%2848758898761%29.jpg/1280px-Raphael%2C_Transfiguration%2C_1518-20%3B_Vatican_Museums_%282%29_%2848758898761%29.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/0/08/Leonardo_da_Vinci_%281452-1519%29_-_The_Last_Supper_%281495-1498%29.jpg/1280px-Leonardo_da_Vinci_%281452-1519%29_-_The_Last_Supper_%281495-1498%29.jpg"
       }),
    
    SORROWFUL("Sorrowful", new String[]{
            "The Agony in the Garden", "The Scourging at the Pillar", "The Crowning with Thorns",
            "The Carrying of the Cross", "The Crucifixion and Death"
    }, new String[]{
            "Jesus prays in Gethsemane, accepting the Father's will in deep anguish.",
            "Jesus is brutally whipped by the Roman soldiers at the command of Pilate.",
            "A crown of thorns is forced onto Jesus' head as soldiers mock his kingship.",
            "Jesus carries the heavy weight of the Cross through the streets of Jerusalem.",
            "Jesus is nailed to the Cross and dies for the redemption of the world."
    },
       new String[]{
            "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a3/Mantegna%2C_Andrea_-_Agony_in_the_Garden_-_National_Gallery%2C_London.jpg/1280px-Mantegna%2C_Andrea_-_Agony_in_the_Garden_-_National_Gallery%2C_London.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ad/The_Flagellation_of_Christ-Caravaggio_%281607%29.jpg/1280px-The_Flagellation_of_Christ-Caravaggio_%281607%29.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/5/52/Michelangelo_Merisi%2C_called_Caravaggio_-_The_Crowning_with_Thorns_-_Google_Art_Project.jpg/1280px-Michelangelo_Merisi%2C_called_Caravaggio_-_The_Crowning_with_Thorns_-_Google_Art_Project.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/3/37/Bosch_-_Christ_Carrying_the_Cross%2C_ca._1510_-_ca._1516%2C_Inv._1902-H.jpg/1280px-Bosch_-_Christ_Carrying_the_Cross%2C_ca._1510_-_ca._1516%2C_Inv._1902-H.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d7/Cristo_crucificado.jpg/1280px-Cristo_crucificado.jpg"
       }),
    
    GLORIOUS("Glorious", new String[]{
            "The Resurrection", "The Ascension", "The Descent of the Holy Spirit",
            "The Assumption", "The Coronation of Mary"
    }, new String[]{
            "Jesus rises from the dead on the third day, conquering sin and death.",
            "Jesus returns to his Father in Heaven, promising to be with us always.",
            "The Holy Spirit descends upon the Apostles and Mary in the Upper Room.",
            "Mary is taken up body and soul into Heavenly glory at the end of her life.",
            "Mary is crowned by her Son as Queen of Heaven and Earth."
    },
       new String[]{
            "https://upload.wikimedia.org/wikipedia/commons/e/eb/Piero_della_Francesca_-_Resurrection_-_WGA17609.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/4/41/Francisco_Camilo_-_Ascension_-_Google_Art_Project.jpg/1280px-Francisco_Camilo_-_Ascension_-_Google_Art_Project.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/5/5e/Santa_Maria_della_Salute_%28Venice%29_-_Discesa_dello_Spirito_Santo_di_Tiziano_%281555%29.jpg/1280px-Santa_Maria_della_Salute_%28Venice%29_-_Discesa_dello_Spirito_Santo_di_Tiziano_%281555%29.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/2/2c/The_Assumption_of_the_Virgin_%28The_Story_of_the_Masterpieces%29.png/1280px-The_Assumption_of_the_Virgin_%28The_Story_of_the_Masterpieces%29.png",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b5/Diego_Vel%C3%A1zquez_-_Coronation_of_the_Virgin_-_Prado.jpg/1280px-Diego_Vel%C3%A1zquez_-_Coronation_of_the_Virgin_-_Prado.jpg"
       });

    private final String displayName;
    private final String[] mysteries;
    private final String[] descriptions;
    private final String[] artworkUrls;

    /**
     * Bundled WebP artwork, indexed by global mystery index (0-19).
     * Loaded from drawable resources so the UI renders instantly, no network.
     */
    private static final int[] ARTWORK_RES_IDS = {
            R.drawable.mystery_joyful_0, R.drawable.mystery_joyful_1,
            R.drawable.mystery_joyful_2, R.drawable.mystery_joyful_3,
            R.drawable.mystery_joyful_4,
            R.drawable.mystery_luminous_0, R.drawable.mystery_luminous_1,
            R.drawable.mystery_luminous_2, R.drawable.mystery_luminous_3,
            R.drawable.mystery_luminous_4,
            R.drawable.mystery_sorrowful_0, R.drawable.mystery_sorrowful_1,
            R.drawable.mystery_sorrowful_2, R.drawable.mystery_sorrowful_3,
            R.drawable.mystery_sorrowful_4,
            R.drawable.mystery_glorious_0, R.drawable.mystery_glorious_1,
            R.drawable.mystery_glorious_2, R.drawable.mystery_glorious_3,
            R.drawable.mystery_glorious_4
    };

    /**
     * Bundled per-mystery audio, indexed by global mystery index (0-19).
     * One file per mystery in res/raw; the service plays each file to completion.
     */
    private static final int[] AUDIO_RES_IDS = {
            R.raw.mystery_joyful_0, R.raw.mystery_joyful_1,
            R.raw.mystery_joyful_2, R.raw.mystery_joyful_3,
            R.raw.mystery_joyful_4,
            R.raw.mystery_luminous_0, R.raw.mystery_luminous_1,
            R.raw.mystery_luminous_2, R.raw.mystery_luminous_3,
            R.raw.mystery_luminous_4,
            R.raw.mystery_sorrowful_0, R.raw.mystery_sorrowful_1,
            R.raw.mystery_sorrowful_2, R.raw.mystery_sorrowful_3,
            R.raw.mystery_sorrowful_4,
            R.raw.mystery_glorious_0, R.raw.mystery_glorious_1,
            R.raw.mystery_glorious_2, R.raw.mystery_glorious_3,
            R.raw.mystery_glorious_4
    };

    MysterySet(String displayName, String[] mysteries, String[] descriptions, String[] artworkUrls) {
        this.displayName = displayName;
        this.mysteries = mysteries;
        this.descriptions = descriptions;
        this.artworkUrls = artworkUrls;
    }

    public String getDisplayName() { return displayName; }
    public String getMystery(int index) { return mysteries[index]; }
    public String getDescription(int index) { return descriptions[index]; }

    /** Raw resource id of the audio file for this set's mystery at the local index (0-4). */
    public int getAudioResId(int localIndex) {
        return AUDIO_RES_IDS[ordinal() * 5 + localIndex];
    }
    public String getArtworkUrl(int index) { return artworkUrls[index]; }

    /** Drawable resource id of the bundled WebP artwork for this mystery. */
    public int getArtworkResId(int index) {
        return ARTWORK_RES_IDS[ordinal() * 5 + index];
    }

    public static MysterySet getLiturgicalSet() {
        int dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        switch (dayOfWeek) {
            case Calendar.MONDAY: case Calendar.SATURDAY: return JOYFUL;
            case Calendar.TUESDAY: case Calendar.FRIDAY: return SORROWFUL;
            case Calendar.WEDNESDAY: case Calendar.SUNDAY: return GLORIOUS;
            case Calendar.THURSDAY: return LUMINOUS;
            default: return JOYFUL;
        }
    }

    /**
     * Returns the full 20-index sequence starting with the day's liturgical set.
     */
    public static int[] getLiturgicalSequence() {
        MysterySet startSet = getLiturgicalSet();
        int[] sequence = new int[20];
        int startOrdinal = startSet.ordinal();
        for (int i = 0; i < 20; i++) {
            int setOffset = i / 5;
            int localIndex = i % 5;
            int currentSetOrdinal = (startOrdinal + setOffset) % 4;
            sequence[i] = currentSetOrdinal * 5 + localIndex;
        }
        return sequence;
    }

    public static MysterySet fromGlobalIndex(int globalIndex) {
        int safeIndex = Math.max(0, Math.min(globalIndex, 19));
        return values()[safeIndex / 5];
    }

    public static int toLocalIndex(int globalIndex) {
        int safeIndex = Math.max(0, Math.min(globalIndex, 19));
        return safeIndex % 5;
    }

    public static int toGlobalIndex(MysterySet set, int localIndex) {
        int safeLocal = Math.max(0, Math.min(localIndex, 4));
        return set.ordinal() * 5 + safeLocal;
    }
}
