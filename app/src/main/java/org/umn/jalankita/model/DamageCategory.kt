package org.umn.jalankita.model

/**
 * Damage categories offered in the report form.
 *
 * [id] is what we persist and upload and must stay in sync with the
 * `reports_category_check` constraint in supabase/schema.sql. Everything else on
 * the entry is user-facing copy: the report form, the example guide and the
 * report detail screen all read their wording from here so one category is
 * described the same way wherever it appears.
 */
enum class DamageCategory(
    val id: String,
    val label: String,
    /** One line explaining what this damage actually looks like on the road. */
    val description: String,
    /** A concrete Indonesian situation, so the category is unmistakable. */
    val example: String,
    /** What to get in the frame, phrased as an instruction. */
    val photoTip: String,
    val severity: DamageSeverity,
) {
    POTHOLE(
        id = "POTHOLE",
        label = "Jalan Berlubang",
        description = "Lubang terbuka pada permukaan jalan, sering menampung air dan " +
            "melebar setiap kali dilewati kendaraan.",
        example = "Lubang selebar ± 50 cm di lajur kiri depan gerbang sekolah, " +
            "cukup dalam untuk menjebak roda sepeda motor.",
        photoTip = "Foto dari jarak ± 2 meter dan sertakan marka atau trotoar " +
            "sebagai pembanding ukuran lubang.",
        severity = DamageSeverity.HIGH,
    ),
    CRACK(
        id = "CRACK",
        label = "Retak Jalan",
        description = "Garis retak memanjang atau menyerupai kulit buaya. Tahap awal " +
            "kerusakan — air masuk lewat retakan dan berubah menjadi lubang.",
        example = "Retak rambut memanjang ± 6 meter di tengah lajur, mulai " +
            "bercabang setelah musim hujan.",
        photoTip = "Ambil tegak lurus dari atas agar pola retakan terlihat jelas, " +
            "bukan dari samping.",
        severity = DamageSeverity.LOW,
    ),
    DAMAGED_ASPHALT(
        id = "DAMAGED_ASPHALT",
        label = "Aspal Rusak",
        description = "Lapisan aspal terkelupas dan batu split lepas, sehingga " +
            "permukaan menjadi kasar dan licin bagi kendaraan roda dua.",
        example = "Aspal mengelupas sepanjang ± 10 meter setelah truk proyek " +
            "melintas, kerikil berserakan di bahu jalan.",
        photoTip = "Sertakan seluruh area yang terkelupas dalam satu bingkai agar " +
            "luas kerusakan terbaca.",
        severity = DamageSeverity.MEDIUM,
    ),
    SUBSIDENCE(
        id = "SUBSIDENCE",
        label = "Jalan Tidak Rata",
        description = "Permukaan bergelombang atau amblas karena tanah di bawahnya " +
            "turun. Berbahaya saat malam karena sulit terlihat.",
        example = "Badan jalan turun ± 15 cm sepanjang satu lajur di atas jalur " +
            "pipa air yang baru digali.",
        photoTip = "Foto dari posisi rendah dan menyamping supaya perbedaan " +
            "ketinggian terlihat.",
        severity = DamageSeverity.HIGH,
    ),
    DAMAGED_SHOULDER(
        id = "DAMAGED_SHOULDER",
        label = "Bahu Jalan Rusak",
        description = "Tepi jalan tergerus atau longsor sehingga lebar jalan yang " +
            "aman dilalui berkurang.",
        example = "Bahu jalan tergerus ± 40 cm setelah saluran air meluap, " +
            "menyisakan tepi aspal yang menggantung.",
        photoTip = "Ambil dari arah memanjang jalan agar bagian tepi yang hilang " +
            "terlihat utuh.",
        severity = DamageSeverity.MEDIUM,
    ),
    FLOODING(
        id = "FLOODING",
        label = "Genangan Air",
        description = "Air menggenang di atas jalan rusak dan menutupi lubang di " +
            "bawahnya — bahaya yang tidak terlihat oleh pengendara.",
        example = "Genangan selebar satu lajur yang tidak surut sejak pagi, " +
            "menutupi lubang di bawah permukaan air.",
        photoTip = "Foto saat air masih menggenang dan tandai perkiraan " +
            "kedalamannya pada deskripsi.",
        severity = DamageSeverity.HIGH,
    ),
    OTHER(
        id = "OTHER",
        label = "Lainnya",
        description = "Kerusakan atau bahaya lain pada jalan yang belum tercakup " +
            "kategori di atas.",
        example = "Penutup gorong-gorong hilang, marka terhapus, atau material " +
            "proyek dibiarkan di badan jalan.",
        photoTip = "Jelaskan pada deskripsi apa yang terlihat pada foto.",
        severity = DamageSeverity.MEDIUM,
    );

    companion object {
        fun fromId(id: String?): DamageCategory = entries.firstOrNull { it.id == id } ?: OTHER

        /**
         * The cases shown in the example guide and on Home, in the order a
         * resident is most likely to meet them.
         */
        val showcase: List<DamageCategory> = listOf(POTHOLE, DAMAGED_ASPHALT, SUBSIDENCE, FLOODING)
    }
}

/** How urgent the damage typically is. Drives the tone of the severity chip. */
enum class DamageSeverity(val label: String) {
    LOW("Ringan"),
    MEDIUM("Sedang"),
    HIGH("Berat"),
}
