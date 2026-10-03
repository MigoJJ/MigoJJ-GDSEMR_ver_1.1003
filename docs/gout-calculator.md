# Gout calculator: criteria and clinical context

Reviewed against the official ACR criteria list on 2026-10-03. The endorsed
ACR/EULAR gout classification score remains the **2015** criteria. This update
keeps that score and adds diagnostic context from the **2018 EULAR diagnosis
recommendations** (published 2020) and **2023 EULAR imaging recommendations**
(published 2024). It does not introduce a new classification year or alter
published weights.

The assessment checks the entry criterion before applying the sufficient
criterion or scoring. Positive MSU findings from symptomatic joint/bursal fluid
or a tophus aspirate meet the sufficient criterion without a numeric score.
Otherwise, a measured serum urate category is required. Unperformed fluid or
imaging examinations are not treated as negative fluid microscopy.

A typical episode requires at least two time-course characteristics; recurrence
is counted only for typical episodes. Ultrasound double contour and DECT share
one imaging score. Ultrasound tophus findings support the newer imaging guidance
but alone do not add 2015 imaging points or imply a clinically apparent tophus.

Below-threshold results indicate failure to meet classification criteria, not
exclusion of gout. The summary distinguishes classification from diagnosis and
includes guidance on the clinical interpretation of imaging and differential
diagnoses. Changing inputs invalidates the previous result before copying or
inserting it into the EMR.

Sources:

- [ACR endorsed criteria list](https://rheumatology.org/criteria)
- [2015 classification criteria, Tables 1–2](https://doi.org/10.1002/art.39254)
- [2018 EULAR diagnosis recommendations](https://doi.org/10.1136/annrheumdis-2019-215315)
- [2023 EULAR imaging recommendations, recommendations 2–3](https://doi.org/10.1136/ard-2023-224771)

Verification: `GoutAssessmentTest` covers applicability, missing values, sufficient
criteria, score domains and threshold boundaries. `GoutAppUiTest` drives the real
JavaFX scene through `FxRobot.interact`, including invalidation of stale results.
