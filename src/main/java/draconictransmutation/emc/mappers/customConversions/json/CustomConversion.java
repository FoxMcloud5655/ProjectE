package draconictransmutation.emc.mappers.customConversions.json;

import java.util.HashMap;
import java.util.Map;

import draconictransmutation.api.nss.NormalizedSimpleStack;

public class CustomConversion {

	public int count = 1;
	public NormalizedSimpleStack output;
	public Map<NormalizedSimpleStack, Integer> ingredients;
	public transient boolean propagateTags = false;

	public static CustomConversion getFor(int count, NormalizedSimpleStack output, Map<NormalizedSimpleStack, Integer> ingredients) {
		CustomConversion conversion = new CustomConversion();
		conversion.count = count;
		conversion.output = output;
		conversion.ingredients = new HashMap<>();
		for (Map.Entry<NormalizedSimpleStack, Integer> entry : ingredients.entrySet()) {
			conversion.ingredients.put(entry.getKey(), entry.getValue());
		}
		return conversion;
	}

	@Override
	public String toString() {
		return "{" + count + " * " + output + " = " + ingredients.toString() + "}";
	}
}