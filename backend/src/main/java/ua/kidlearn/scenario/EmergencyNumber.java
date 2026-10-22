package ua.kidlearn.scenario;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * The one verified source of emergency numbers: the table of docs/Вимоги_модуль_Безпека.md §6.
 * Given to the LLM as VERIFIED_FACTS and enforced by ScenarioValidator — a lesson may name no
 * other number, so a number "from the model's memory" never reaches a child. Re-check against
 * official sources (ДСНС / the government) before a public launch, as that document requires.
 */
public enum EmergencyNumber {

	FIRE("101", "Пожежно-рятувальна служба (ДСНС)", "дим, вогонь, людина в небезпеці (застрягла, тоне)"),
	POLICE("102", "Поліція", "загроза від людей, крадіжка, загубився"),
	AMBULANCE("103", "Екстрена медична допомога", "людині дуже погано, травма, не прокидається"),
	GAS("104", "Аварійна служба газу", "запах газу"),
	GENERAL("112", "Єдиний номер екстреної допомоги", "будь-яка біда, якщо не знаєш куди / як загальний номер");

	private final String number;
	private final String service;
	private final String when;

	EmergencyNumber(String number, String service, String when) {
		this.number = number;
		this.service = service;
		this.when = when;
	}

	public String number() {
		return number;
	}

	public static boolean isVerified(String number) {
		return Arrays.stream(values()).anyMatch(n -> n.number.equals(number));
	}

	/** The table as the prompt's VERIFIED_FACTS block lists it (docs/Промпт_генерації_уроків.md §2). */
	public static String factsBlock() {
		return Arrays.stream(values())
				.map(n -> "- " + n.number + " — " + n.service + ": " + n.when)
				.collect(Collectors.joining("\n"))
				+ "\n- Дзвінки на ці номери безкоштовні й працюють без грошей на рахунку.";
	}

}
