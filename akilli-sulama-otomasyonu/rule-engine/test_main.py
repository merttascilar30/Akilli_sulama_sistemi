import unittest
from main import evaluate_rule, KuralIstek, vana_durumlari

class TestKuralMotoru(unittest.IsolatedAsyncioTestCase):
    def setUp(self):
        # Her test senaryosundan önce bellek durumunu temizliyoruz
        vana_durumlari.clear()

    async def test_evaluate_rule_sulama_gerekli(self):
        # Senaryo 1: Toprak anlık nemi dinamik eşiğin çok altında, vana açılmalı
        istek = KuralIstek(
            istasyonId="istasyon-test-1",
            anlikNem=22.0,
            tarlaKapasitesi=40.0,
            solmaNoktasi=20.0,
            et0=5.0,
            kc=0.85
        )
        response = await evaluate_rule(istek)
        self.assertTrue(response["sulamaGerekli"])
        self.assertEqual(vana_durumlari["istasyon-test-1"], True)

    async def test_evaluate_rule_histerezis_durumu(self):
        # Senaryo 2: Vana zaten AÇIK. Nem oranı eşiğin (%31.7) üstünde ama Tarla Kapasitesinin (%40) altında.
        # Histerezis gereği toprak suya doyana kadar vana AÇIK kalmaya devam etmeli.
        vana_durumlari["istasyon-test-1"] = True
        
        istek = KuralIstek(
            istasyonId="istasyon-test-1",
            anlikNem=35.0, 
            tarlaKapasitesi=40.0,
            solmaNoktasi=20.0,
            et0=5.0,
            kc=0.85
        )
        response = await evaluate_rule(istek)
        self.assertTrue(response["sulamaGerekli"])
        self.assertEqual(vana_durumlari["istasyon-test-1"], True)

    async def test_evaluate_rule_vana_kapatma(self):
        # Senaryo 3: Vana AÇIK durumdayken nem oranı Tarla Kapasitesini (%40) geçiyor.
        # Toprak suya doyduğu için vana artık kapanmalı (False dönmeli).
        vana_durumlari["istasyon-test-1"] = True
        
        istek = KuralIstek(
            istasyonId="istasyon-test-1",
            anlikNem=42.0,
            tarlaKapasitesi=40.0,
            solmaNoktasi=20.0,
            et0=5.0,
            kc=0.85
        )
        response = await evaluate_rule(istek)
        self.assertFalse(response["sulamaGerekli"])
        self.assertEqual(vana_durumlari["istasyon-test-1"], False)

if __name__ == "__main__":
    unittest.main()