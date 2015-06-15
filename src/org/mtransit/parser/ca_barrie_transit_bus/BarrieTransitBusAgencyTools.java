package org.mtransit.parser.ca_barrie_transit_bus;

import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MSpec;
import org.mtransit.parser.mt.data.MTrip;

// http://www.barrie.ca/Living/Getting%20Around/BarrieTransit/Pages/Barrie-GTFS.aspx
// http://transit.cityofbarriesites.com/files/google_transit.zip
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
		System.out.printf("Generating Barrie Transit bus data...\n");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("Generating Barrie Transit bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
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

	@Override
	public long getRouteId(GRoute gRoute) {
		Matcher matcher = DIGITS.matcher(gRoute.route_short_name);
		matcher.find();
		return Long.parseLong(matcher.group());
	}

	@Override
	public String getRouteShortName(GRoute gRoute) {
		Matcher matcher = DIGITS.matcher(gRoute.route_short_name);
		matcher.find();
		return matcher.group();
	}

	@Override
	public String getRouteLongName(GRoute gRoute) {
		String routeLongName = gRoute.route_long_name;
		routeLongName = routeLongName.toLowerCase(Locale.ENGLISH);
		return MSpec.cleanLabel(routeLongName);
	}

	private static final String AGENCY_COLOR_BLUE = "336699"; // BLUE (from web site CSS)
	private static final String AGENCY_COLOR = AGENCY_COLOR_BLUE;

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Override
	public String getRouteColor(GRoute gRoute) {
		Matcher matcher = DIGITS.matcher(gRoute.route_short_name);
		matcher.find();
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
		case 90: return "007236";
		// @formatter:on
		}
		System.out.println("Unexpected route color " + gRoute);
		System.exit(-1);
		return null;
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		GRoute gRoute = gtfs.routes.get(gTrip.getRouteId());
		String rsn = gRoute.route_short_name;
		String rsn_letter = rsn.substring(rsn.length() - 1, rsn.length());
		String tripHeadsign = rsn_letter + " " + getRouteLongName(gRoute);
		int directionId;
		if (A.equals(rsn_letter)) {
			directionId = 0;
		} else if (B.equals(rsn_letter)) {
			directionId = 1;
		} else {
			System.out.printf("Unexpected trip (unexpected rsn: %s): %s\n", rsn, gTrip);
			System.exit(-1);
			directionId = -1;
		}
		mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), directionId);
	}

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = MSpec.cleanStreetTypes(tripHeadsign);
		return MSpec.cleanLabel(tripHeadsign);
	}

	private static final Pattern AT = Pattern.compile("( at )", Pattern.CASE_INSENSITIVE);

	private static final String AT_REPLACEMENT = " / ";

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = AT.matcher(gStopName).replaceAll(AT_REPLACEMENT);
		gStopName = MSpec.cleanStreetTypes(gStopName);
		return MSpec.cleanLabel(gStopName);
	}

	@Override
	public int getStopId(GStop gStop) {
		return Integer.parseInt(gStop.stop_code); // use stop code as stop ID
	}
}
