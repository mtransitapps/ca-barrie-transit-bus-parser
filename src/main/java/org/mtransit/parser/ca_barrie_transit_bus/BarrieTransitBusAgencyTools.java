package org.mtransit.parser.ca_barrie_transit_bus;

import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-barrie-transit-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new BarrieTransitBusAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		MTLog.log("Generating Barrie Transit bus data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this, true);
		super.start(args);
		MTLog.log("Generating Barrie Transit bus data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIds != null && this.serviceIds.isEmpty();
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

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
	public long getRouteId(GRoute gRoute) {
		if (Utils.isDigitsOnly(gRoute.getRouteShortName())) {
			return Long.parseLong(gRoute.getRouteShortName()); // use route short name as route ID
		}
		Matcher matcher = DIGITS.matcher(gRoute.getRouteShortName());
		if (matcher.find()) {
			return Long.parseLong(matcher.group()); // merge routes
		}
		MTLog.logFatal("Unexpected route ID %s!", gRoute);
		return -1L;
	}

	@Override
	public String getRouteShortName(GRoute gRoute) {
		Matcher matcher = DIGITS.matcher(gRoute.getRouteShortName());
		if (matcher.find()) {
			return matcher.group(); // merge routes
		}
		MTLog.logFatal("Unexpected route short name for %s!", gRoute);
		return null;
	}

	@Override
	public String getRouteLongName(GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongName();
		if (Utils.isUppercaseOnly(routeLongName, true, true)) {
			routeLongName = routeLongName.toLowerCase(Locale.ENGLISH);
		}
		return CleanUtils.cleanLabel(routeLongName);
	}

	@Override
	public boolean mergeRouteLongName(MRoute mRoute, MRoute mRouteToMerge) {
		if (mRoute.getId() == 100L) {
			mRoute.setLongName("Georgian Express");
			return true;
		}
		return super.mergeRouteLongName(mRoute, mRouteToMerge);
	}

	private static final String AGENCY_COLOR_BLUE = "336699"; // BLUE (from web site CSS)
	private static final String AGENCY_COLOR = AGENCY_COLOR_BLUE;

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Override
	public String getRouteColor(GRoute gRoute) {
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
		MTLog.logFatal("Unexpected route color %s!", gRoute);
		return null;
	}

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;

	static {
		//noinspection UnnecessaryLocalVariable
		HashMap<Long, RouteTripSpec> map2 = new HashMap<>();
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	private int priscillasPlaceId = -1;
	private int pkPlId = -1;

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		if (mRoute.getId() == 11L) {
			switch (gTrip.getTripHeadsign()) {
			case "Priscillas Place":
				if (this.priscillasPlaceId < 0) {
					if (this.pkPlId < 0) {
						this.priscillasPlaceId = gTrip.getDirectionId();
					} else if (this.pkPlId == 0) {
						this.priscillasPlaceId = 1;
					} else if (this.pkPlId == 1) {
						this.priscillasPlaceId = 0;
					} else {
						MTLog.log("%s: Unexpected trip: %s", mRoute.getShortName(), gTrip);
						MTLog.log("%s: Unexpected this.pkPlId: %s", mRoute.getShortName(), this.pkPlId);
						MTLog.logFatal("%s: Unexpected this.priscillasPlaceId: %s", mRoute.getShortName(), this.priscillasPlaceId);
					}
				}
				mTrip.setHeadsignString("Priscillas Pl", this.priscillasPlaceId);
				return;
			case "A Rec to Lockhart":
				if (this.priscillasPlaceId < 0) {
					if (this.pkPlId < 0) {
						this.priscillasPlaceId = gTrip.getDirectionId();
					} else if (this.pkPlId == 0) {
						this.priscillasPlaceId = 1;
					} else if (this.pkPlId == 1) {
						this.priscillasPlaceId = 0;
					} else {
						MTLog.log("%s: Unexpected trip: %s", mRoute.getShortName(), gTrip);
						MTLog.log("%s: Unexpected this.pkPlId: %s", mRoute.getShortName(), this.pkPlId);
						MTLog.logFatal("%s: Unexpected this.priscillasPlaceId: %s", mRoute.getShortName(), this.priscillasPlaceId);
					}
				}
				mTrip.setHeadsignString("Lockhart", this.priscillasPlaceId);
				return;
			case "PP":
				if (this.pkPlId < 0) {
					if (this.priscillasPlaceId < 0) {
						this.pkPlId = gTrip.getDirectionId();
					} else if (this.priscillasPlaceId == 0) {
						this.pkPlId = 1;
					} else if (this.priscillasPlaceId == 1) {
						this.pkPlId = 0;
					} else {
						MTLog.log("%s: Unexpected trip: %s", mRoute.getShortName(), gTrip);
						MTLog.log("%s: Unexpected this.priscillasPlaceId: %s", mRoute.getShortName(), this.priscillasPlaceId);
						MTLog.logFatal("%s: Unexpected this.pkPlId: %s", mRoute.getShortName(), this.pkPlId);
					}
				}
				mTrip.setHeadsignString("Pk Pl", this.pkPlId);
				return;
			case "Allandale Rec":
				if (this.pkPlId < 0) {
					if (this.priscillasPlaceId < 0) {
						this.pkPlId = gTrip.getDirectionId();
					} else if (this.priscillasPlaceId == 0) {
						this.pkPlId = 1;
					} else if (this.priscillasPlaceId == 1) {
						this.pkPlId = 0;
					} else {
						MTLog.log("%s: Unexpected trip: %s", mRoute.getShortName(), gTrip);
						MTLog.log("%s: Unexpected this.priscillasPlaceId: %s", mRoute.getShortName(), this.priscillasPlaceId);
						MTLog.logFatal("%s: Unexpected this.pkPlId: %s", mRoute.getShortName(), this.pkPlId);
					}
				}
				mTrip.setHeadsignString("Allandale Rec", this.pkPlId);
				return;
			}
			MTLog.log("%s: Unexpected trip: %s", mRoute.getShortName(), gTrip);
			MTLog.log("%s: Unexpected gTrip.getDirectionId(): %s", mRoute.getShortName(), gTrip.getDirectionId());
			MTLog.logFatal("%s: Unexpected gTrip.getTripHeadsign(): %s", mRoute.getShortName(), gTrip.getTripHeadsign());
			return;
		}
		GRoute gRoute = gtfs.getRoute(gTrip.getRouteId());
		String rsn = gRoute.getRouteShortName().trim();
		String rsn_letter = rsn.substring(rsn.length() - 1);
		String tripHeadsign = rsn_letter + " " + getRouteLongName(gRoute);
		int directionId;
		if (A.equals(rsn_letter) || //
				C.equals(rsn_letter)) {
			directionId = 0;
		} else if (B.equals(rsn_letter) || //
				D.equals(rsn_letter)) {
			directionId = 1;
		} else {
			MTLog.logFatal("%s: Unexpected trip: %s", mRoute.getShortName(), gTrip);
			return;
		}
		mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), directionId);
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
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
		MTLog.logFatal("Unexpected trips to merge: %s & %s!", mTrip, mTripToMerge);
		return false;
	}

	private static final Pattern STARTS_WITH_TO = Pattern.compile("(([A-Z])?(.*)( to )(.*))", Pattern.CASE_INSENSITIVE);
	private static final String STARTS_WITH_TO_REPLACEMENT = "$2 $5";

	private static final Pattern DBT_ = CleanUtils.cleanWords("dbt");
	private static final String DBT_REPLACEMENT = CleanUtils.cleanWordsReplacement("DBT");

	private static final Pattern GC_ = CleanUtils.cleanWords("gc");
	private static final String GC_REPLACEMENT = CleanUtils.cleanWordsReplacement("GC");

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = STARTS_WITH_TO.matcher(tripHeadsign).replaceAll(STARTS_WITH_TO_REPLACEMENT);
		tripHeadsign = DBT_.matcher(tripHeadsign).replaceAll(DBT_REPLACEMENT);
		tripHeadsign = GC_.matcher(tripHeadsign).replaceAll(GC_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = CleanUtils.CLEAN_AT.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	@Override
	public int getStopId(GStop gStop) {
		String stopCode = gStop.getStopCode();
		if (Utils.isDigitsOnly(stopCode)) {
			return Integer.parseInt(stopCode); // use stop code as stop ID
		}
		Matcher matcher = DIGITS.matcher(stopCode);
		if (matcher.find()) {
			int digits = Integer.parseInt(matcher.group());
			int stopId;
			if (stopCode.startsWith("AG ")) {
				stopId = 100_000;
			} else {
				MTLog.logFatal("Stop doesn't have an ID (start with)! %s", gStop);
				return -1;
			}
			return stopId + digits;
		}
		MTLog.logFatal("Unexpected stop ID for %s!", gStop);
		return -1;
	}
}
