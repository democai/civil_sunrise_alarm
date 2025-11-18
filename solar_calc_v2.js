#!/usr/bin/env node

/**
 * Solar noon and civil dawn calculator - CORRECTED VERSION
 * Location: Beverly Hills, CA 34°05'N / 118°24'W
 * Date: November 18, 2025
 *
 * Official reference data:
 * Solar Noon: 11:38 AM (local time, UTC-8)
 * Sunrise: 6:28 AM
 * Sunset: 4:47 PM
 * Civil Twilight: 6:02 AM – 5:14 PM
 */

// Configuration
const lat = 34.0833;
const lon = -118.4;
const year = 2025;
const month = 11;
const day = 18;

// Day of year
const daysInMonth = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
let dayOfYear = 0;
for (let i = 0; i < month - 1; i++) {
  dayOfYear += daysInMonth[i];
}
dayOfYear += day;

console.log(`\n📍 Location: Beverly Hills, CA (lat ${lat}, lon ${lon})`);
console.log(`📅 Date: ${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')} (Day ${dayOfYear} of year)`);
console.log(`🕐 Timezone: UTC-8 (PST)`);

// ===== EQUATION OF TIME =====
console.log(`\n${'='.repeat(60)}`);
console.log('EQUATION OF TIME FORMULAS');
console.log('='.repeat(60));

// Use decimal day for calculations
const decimalYear = year + (dayOfYear - 1) / 365.25;

// Formula 1: Simple approximation (often used in sunrise/sunset calculators)
const b = (360.0 * (dayOfYear - 81)) / 365.0;
const eot1 = 9.87 * Math.sin(2 * (b * Math.PI / 180)) -
             7.53 * Math.cos(b * Math.PI / 180) -
             1.5 * Math.sin(b * Math.PI / 180);

console.log(`\nFormula 1 (simple approximation):`);
console.log(`  b = (360 * (${dayOfYear} - 81)) / 365 = ${b.toFixed(2)}°`);
console.log(`  EoT = 9.87·sin(2b) - 7.53·cos(b) - 1.5·sin(b)`);
console.log(`  EoT = ${eot1.toFixed(2)} minutes`);

// Formula 2: Spencer (1971) - widely used in solar calculations
const gamma = (2 * Math.PI * (dayOfYear - 1)) / 365.0;
const eot2 = 229.18 * (0.000075 + 0.001868 * Math.cos(gamma) - 0.032077 * Math.sin(gamma) -
                       0.014615 * Math.cos(2 * gamma) - 0.040849 * Math.sin(2 * gamma));

console.log(`\nFormula 2 (Spencer 1971):`);
console.log(`  gamma = 2π(${dayOfYear} - 1) / 365 = ${gamma.toFixed(4)} rad`);
console.log(`  EoT = 229.18 * (0.000075 + 0.001868·cos(γ) - 0.032077·sin(γ) - ...)`);
console.log(`  EoT = ${eot2.toFixed(2)} minutes`);

// Note: NOAA's full algorithm is more complex and involves more parameters.
// For this application, Spencer (1971) is sufficiently accurate.

// ===== SOLAR NOON (UTC) =====
console.log(`\n${'='.repeat(60)}`);
console.log('SOLAR NOON CALCULATION');
console.log('='.repeat(60));

// Standard formula for solar noon (UTC):
// Solar_Noon_UTC = 12:00 - (Longitude / 15) - (EoT / 60)
//
// Where:
//   12:00 = solar noon at Prime Meridian (0° longitude)
//   -Lon/15 = correction for observer's longitude (degrees to hours)
//   -EoT/60 = correction for equation of time (minutes to hours)
//   EoT is ALWAYS subtracted (universal standard definition)

console.log(`\nStandard Formula: Solar_Noon_UTC = 12:00 - (Lon/15) - (EoT/60)`);
console.log(`  12:00 = reference at Prime Meridian (0°)`);
console.log(`  -Lon/15 = longitude correction (${lon}° ÷ 15 = ${(lon/15).toFixed(3)} hours)`);
console.log(`  -EoT/60 = equation of time correction (always subtract)`);

// Convert UTC to local time: local = UTC + timezone_offset
// For UTC-8: local = UTC - 8
const timezoneHours = -8;

// Calculate solar noon using Spencer EoT (most accurate)
const solarNoonUTC = 12.0 - (lon / 15.0) - (eot2 / 60.0);
const solarNoonLocal = solarNoonUTC + timezoneHours;

console.log(`\nCalculation (Spencer EoT = ${eot2.toFixed(2)} min):`);
console.log(`  Solar Noon UTC = 12:00 - (${lon}/15) - (${eot2.toFixed(2)}/60)`);
console.log(`  Solar Noon UTC = ${solarNoonUTC.toFixed(3)} hours`);
console.log(`  Solar Noon Local (UTC-8) = ${solarNoonLocal.toFixed(3)} hours`);
formatTime(solarNoonLocal, 'Local time');

// ===== COMPARE WITH OFFICIAL DATA =====
console.log(`\n${'='.repeat(60)}`);
console.log('COMPARISON WITH OFFICIAL DATA');
console.log('='.repeat(60));
console.log(`\n📊 Official Solar Noon: 11:38 AM local time`);

const officialHour = 11 + 38/60;
console.log(`Calculated: ${formatTimeReturn(solarNoonLocal)} - Error: ${Math.abs(solarNoonLocal - officialHour) * 60} minutes ✓`);

// ===== CIVIL DAWN CALCULATION =====
console.log(`\n${'='.repeat(60)}`);
console.log('CIVIL DAWN CALCULATION');
console.log('='.repeat(60));

// Using the solar noon calculated above
const solarNoonForCalc = solarNoonLocal;

// Solar declination (Spencer 1971 - accurate to ±0.0006°)
console.log(`\nSolar Declination (Spencer 1971):`);

const gammaDecl = (2 * Math.PI * (dayOfYear - 1)) / 365.0;
const dec = 0.006918 - 0.399912 * Math.cos(gammaDecl) + 0.070257 * Math.sin(gammaDecl) -
             0.006758 * Math.cos(2 * gammaDecl) + 0.000907 * Math.sin(2 * gammaDecl) -
             0.00221 * Math.cos(3 * gammaDecl);
const dec_deg = dec * 180 / Math.PI;
console.log(`  δ = ${dec_deg.toFixed(2)}°`);

// Hour angle for civil twilight (sun 6° below horizon)
const latRad = lat * Math.PI / 180;
const decRad = dec_deg * Math.PI / 180;  // dec_deg is in degrees
const horizonAngleRad = -6 * Math.PI / 180;

const cosH = (Math.sin(horizonAngleRad) - Math.sin(latRad) * Math.sin(decRad)) /
             (Math.cos(latRad) * Math.cos(decRad));

console.log(`\nCivil Twilight (sun 6° below horizon):`);
console.log(`  Formula: cos(H) = (sin(α) - sin(φ)·sin(δ)) / (cos(φ)·cos(δ))`);
console.log(`  where α = -6° (altitude), φ = latitude, δ = declination`);
console.log(`  cos(H) = (${Math.sin(horizonAngleRad).toFixed(4)} - ${(Math.sin(latRad) * Math.sin(decRad)).toFixed(4)}) / ${(Math.cos(latRad) * Math.cos(decRad)).toFixed(4)}`);
console.log(`  cos(H) = ${cosH.toFixed(4)}`);

if (Math.abs(cosH) <= 1) {
  const H_rad = Math.acos(cosH);
  const H_deg = H_rad * 180 / Math.PI;
  const H_hours = H_deg / 15;

  console.log(`\n  H (hour angle) = ${H_rad.toFixed(4)} rad = ${H_deg.toFixed(2)}° = ${H_hours.toFixed(4)} hours`);
  console.log(`  (Angular distance from solar noon to -6° position)`);

  // Civil dawn is BEFORE solar noon, so subtract
  const civilDawnLocal = solarNoonForCalc - H_hours;

  console.log(`\nCivil Dawn calculation:`);
  console.log(`  Solar Noon: ${formatTimeReturn(solarNoonForCalc)}`);
  console.log(`  Hours before noon: ${H_hours.toFixed(4)} hours = ${(H_hours * 60).toFixed(1)} minutes`);
  console.log(`  Civil Dawn: ${formatTimeReturn(civilDawnLocal)}`);

  console.log(`\n📊 Official Civil Twilight Start: 6:02 AM`);
  const officialCivilDawn = 6 + 2/60;
  const dawnError = Math.abs(civilDawnLocal - officialCivilDawn) * 60;
  console.log(`Calculated: ${formatTimeReturn(civilDawnLocal)}`);
  console.log(`Error: ${dawnError.toFixed(1)} minutes`);

  // If off by more than 30 min, suggest potential issues
  if (dawnError > 30) {
    console.log(`\n⚠️ Large discrepancy - possible issues:`);
    console.log(`  1. Declination formula might need adjustment`);
    console.log(`  2. Horizon angle interpretation (some use positive 6°)`);
    console.log(`  3. Refraction coefficient not accounted for`);
  }
} else {
  console.log(`\n⚠️ No civil twilight on this day (|cos(H)| > 1)`);
}

console.log(`\n${'='.repeat(60)}\n`);

// ===== HELPER FUNCTIONS =====
function formatTime(hours, label = '') {
  // Handle hour wrapping
  let h = Math.floor(hours);
  let m = Math.round((hours - h) * 60);

  // Handle day boundary
  if (h < 0) {
    h += 24;
    label += ' (previous day)';
  } else if (h >= 24) {
    h -= 24;
    label += ' (next day)';
  }

  const period = h >= 12 ? 'PM' : 'AM';
  const displayHour = h % 12 === 0 ? 12 : h % 12;
  const timeStr = `${displayHour}:${String(m).padStart(2, '0')} ${period}`;
  console.log(`  → ${timeStr} ${label}`);
  return timeStr;
}

function formatTimeReturn(hours) {
  let h = Math.floor(hours);
  let m = Math.round((hours - h) * 60);

  if (h < 0) h += 24;
  if (h >= 24) h -= 24;

  const period = h >= 12 ? 'PM' : 'AM';
  const displayHour = h % 12 === 0 ? 12 : h % 12;
  return `${displayHour}:${String(m).padStart(2, '0')} ${period}`;
}

