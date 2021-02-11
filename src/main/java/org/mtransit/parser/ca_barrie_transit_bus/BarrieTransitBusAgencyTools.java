package org.mtransit.parser.ca_barrie_transit_bus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.CharUtils;
import org.mtransit.commons.CleanUtils;
import org.mtransit.commons.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// http://www.barrie.ca/Living/Getting%20Around/BarrieTransit/Pages/Barrie-GTFS.aspx
// http://transit.cityofbarriesites.com/files/google_transit.zip
// http://www.myridebarrie.ca/gtfs/
// http://www.myridebarrie.ca/gtfs/google_transit.zip
public class BarrieTransitBusAgencyTools extends DefaultAgencyTools {

	public static void main(@Nullable String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-barrie-transit-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new BarrieTransitBusAgencyTools().start(args);
	}

	@Nullable
	private HashSet<Integer> serviceIds;

	@Override
	public void start(@NotNull String[] args) {
		MTLog.log("Generating Barrie Transit bus data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIdInts(args, this, true);
		super.start(args);
		MTLog.log("Generating Barrie Transit bus data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIds != null && this.serviceIds.isEmpty();
	}

	@Override
	public boolean excludeCalendar(@NotNull GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarInt(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(@NotNull GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDateInt(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(@NotNull GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTripInt(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	private static final String A = "A";
	private static final String B = "B";
	private static final String C = "C";
	private static final String D = "D";

	@Override
	public long getRouteId(@NotNull GRoute gRoute) {
		final String rsn = gRoute.getRouteShortName();
		if (CharUtils.isDigitsOnly(rsn)) {
			return Long.parseLong(rsn); // use route short name as route ID
		}
		final Matcher matcher = DIGITS.matcher(rsn);
		if (matcher.find()) {
			return Long.parseLong(matcher.group()); // merge routes
		}
		throw new MTLog.Fatal("Unexpected route ID for %s!", gRoute);
	}

	@Nullable
	@Override
	public String getRouteShortName(@NotNull GRoute gRoute) {
		final Matcher matcher = DIGITS.matcher(gRoute.getRouteShortName());
		if (matcher.find()) {
			return matcher.group(); // merge routes
		}
		throw new MTLog.Fatal("Unexpected route short name for %s!", gRoute);
	}

	@NotNull
	@Override
	public String getRouteLongName(@NotNull GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongNameOrDefault();
		if (CharUtils.isUppercaseOnly(routeLongName, true, true)) {
			routeLongName = routeLongName.toLowerCase(Locale.ENGLISH);
		}
		return CleanUtils.cleanLabel(routeLongName);
	}

	@Override
	public boolean mergeRouteLongName(@NotNull MRoute mRoute, @NotNull MRoute mRouteToMerge) {
		if (mRoute.getId() == 100L) {
			mRoute.setLongName("Georgian Express");
			return true;
		}
		return super.mergeRouteLongName(mRoute, mRouteToMerge);
	}

	private static final String AGENCY_COLOR_BLUE = "336699"; // BLUE (from web site CSS)
	private static final String AGENCY_COLOR = AGENCY_COLOR_BLUE;

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Nullable
	@Override
	public String getRouteColor(@NotNull GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteColor())) {
			Matcher matcher = DIGITS.matcher(gRoute.getRouteShortName());
			if (matcher.find()) {
				int routeId = Integer.parseInt(matcher.group());
				switch (routeId) {
				// @formatter:off
				case 1: return "EC008C";
				case 2: return "ED1C24";
				case 3: return "0089CF";
				case 4: return "918BC3";
				case 5: return "8ED8F8";
				case 6: return "B2D235";
				case 7: return "F58220";
				case 8: return "000000";
				case 11: return "FFFF00";
				case 90: return "007236";
				case 100: return "57AD40";
				// @formatter:on
				}
			}
			throw new MTLog.Fatal("Unexpected route color %s!", gRoute);
		}
		return super.getRouteColor(gRoute);
	}

	@Override
	public void setTripHeadsign(@NotNull MRoute mRoute, @NotNull MTrip mTrip, @NotNull GTrip gTrip, @NotNull GSpec gtfs) {
		//noinspection deprecation
		GRoute gRoute = gtfs.getRoute(gTrip.getRouteId());
		if (gRoute == null) {
			throw new MTLog.Fatal("%s: Unexpected trip: %s", mRoute.getShortName(), gTrip);
		}
		String rsn = gRoute.getRouteShortName().trim();
		String rsn_letter = rsn.substring(rsn.length() - 1);
		String tripHeadsign = rsn_letter + " " + getRouteLongName(gRoute);
		int directionId;
		if (A.equals(rsn_letter)
				|| C.equals(rsn_letter)) {
			directionId = 0;
		} else if (B.equals(rsn_letter)
				|| D.equals(rsn_letter)) {
			directionId = 1;
		} else {
			throw new MTLog.Fatal("%s: Unexpected trip: %s", mRoute.getShortName(), gTrip);
		}
		mTrip.setHeadsignString(
				cleanTripHeadsign(tripHeadsign),
				directionId
		);
	}

	@Override
	public boolean directionFinderEnabled() {
		return false; // disabled because 2 GTFS routes = 1 route & provided direction ID invalid/useless (same direction ID for 2 distinct directions)
	}

	@Override
	public boolean mergeHeadsign(@NotNull MTrip mTrip, @NotNull MTrip mTripToMerge) {
		List<String> headsignsValues = Arrays.asList(mTrip.getHeadsignValue(), mTripToMerge.getHeadsignValue());
		if (mTrip.getRouteId() == 11L) {
			if (Arrays.asList( //
					"Pk Pl", //
					"Allendale Rec" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Allendale Rec", mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					"Priscillas Pl", //
					"Lockhart" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Lockhart", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 100L) {
			if (Arrays.asList( //
					"A " + "GC", //
					"A " + "G.C.", //
					"C " + "Kozlov Mall", //
					"C " + "G.M. Via G.C.", //
					"Kozlov Mall" // ++
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Kozlov Mall", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					"A " + "Red Express", //
					"C " + "Red Express",//
					"Red Express" // ++
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Red Express", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					"B " + "DBT", //
					"B " + "D.T.", //
					"D " + "DBT", //
					"D " + "D.T. Via G.C.",//
					"DBT" // ++
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("DBT", mTrip.getHeadsignId()); // Downtown Barrie Terminal
				return true;
			}
			if (Arrays.asList( //
					"B " + "Blue Express", //
					"D " + "Blue Express",//
					"Blue Express" // ++
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Blue Express", mTrip.getHeadsignId());
				return true;
			}
		}
		throw new MTLog.Fatal("Unexpected trips to merge: %s & %s!", mTrip, mTripToMerge);
	}

	private static final Pattern DBT_ = CleanUtils.cleanWords("dbt");
	private static final String DBT_REPLACEMENT = CleanUtils.cleanWordsReplacement("DBT");

	private static final Pattern GC_ = CleanUtils.cleanWords("gc");
	private static final String GC_REPLACEMENT = CleanUtils.cleanWordsReplacement("GC");

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = CleanUtils.keepToAndRemoveVia(tripHeadsign);
		tripHeadsign = DBT_.matcher(tripHeadsign).replaceAll(DBT_REPLACEMENT);
		tripHeadsign = GC_.matcher(tripHeadsign).replaceAll(GC_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = CleanUtils.CLEAN_AT.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	@Override
	public int getStopId(@NotNull GStop gStop) {
		String stopCode = gStop.getStopCode();
		if (CharUtils.isDigitsOnly(stopCode)) {
			return Integer.parseInt(stopCode); // use stop code as stop ID
		}
		Matcher matcher = DIGITS.matcher(stopCode);
		if (matcher.find()) {
			int digits = Integer.parseInt(matcher.group());
			int stopId;
			if (stopCode.startsWith("AG ")) {
				stopId = 100_000;
			} else {
				throw new MTLog.Fatal("Stop doesn't have an ID (start with)! %s", gStop);
			}
			return stopId + digits;
		}
		throw new MTLog.Fatal("Unexpected stop ID for %s!", gStop);
	}
}
