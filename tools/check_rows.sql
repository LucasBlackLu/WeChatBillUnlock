.mode list
SELECT 'payment rows total: ' || count(*) FROM scene_rule WHERE scene='flashNotes_payment';
SELECT 'appbrand rows: ' || count(*) FROM scene_rule WHERE scene='flashNotes_payment' AND pageControlName LIKE '%AppBrand%';
