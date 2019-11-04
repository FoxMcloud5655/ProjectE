package moze_intel.projecte.emc;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.event.EMCRemapEvent;
import moze_intel.projecte.api.mapper.IEMCMapper;
import moze_intel.projecte.api.mapper.arithmetic.IValueArithmetic;
import moze_intel.projecte.api.mapper.collector.IExtendedMappingCollector;
import moze_intel.projecte.api.mapper.generator.IValueGenerator;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.emc.arithmetic.HiddenBigFractionArithmetic;
import moze_intel.projecte.emc.collector.DumpToFileCollector;
import moze_intel.projecte.emc.collector.LongToBigFractionCollector;
import moze_intel.projecte.emc.generator.BigFractionToLongGenerator;
import moze_intel.projecte.emc.mappers.TagMapper;
import moze_intel.projecte.emc.pregenerated.PregeneratedEMC;
import moze_intel.projecte.playerData.Transmutation;
import moze_intel.projecte.utils.AnnotationHelper;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.IItemProvider;
import net.minecraftforge.common.MinecraftForge;
import org.apache.commons.math3.fraction.BigFraction;

public final class EMCMappingHandler {

	private static final List<IEMCMapper<NormalizedSimpleStack, Long>> EMC_MAPPERS = new ArrayList<>();
	//TODO: Evaluate LinkedHashMap vs HashMap
	public static final Map<ItemInfo, Long> emc = new LinkedHashMap<>();
	public static double covalenceLoss = ProjectEConfig.difficulty.covalenceLoss.get();
	public static boolean covalenceLossRounding = ProjectEConfig.difficulty.covalenceLossRounding.get();

	private static void loadMappers() {
		//If we don't have any mappers loaded try to load them
		if (EMC_MAPPERS.isEmpty()) {
			//Add all the EMC mappers we have encountered
			EMC_MAPPERS.addAll(AnnotationHelper.getEMCMappers());
			//Manually register the Tag Mapper to ensure that it is registered last so that it can "fix" all the tags used in any of the other mappers
			// This also has the side effect to make sure that we can use EMC_MAPPERS.isEmpty to check if we have attempted to initialize our cache yet
			EMC_MAPPERS.add(new TagMapper());
		}
	}

	public static <T> T getOrSetDefault(CommentedFileConfig config, String key, String comment, T defaultValue) {
		T val = config.get(key);
		if (val == null) {
			val = defaultValue;
			config.set(key, val);
			config.setComment(key, comment);
		}
		return val;
	}

	public static void map(IResourceManager resourceManager) {
		//Ensure we load the EMC mappers
		loadMappers();
		SimpleGraphMapper<NormalizedSimpleStack, BigFraction, IValueArithmetic<BigFraction>> mapper = new SimpleGraphMapper<>(new HiddenBigFractionArithmetic());
		IValueGenerator<NormalizedSimpleStack, Long> valueGenerator = new BigFractionToLongGenerator<>(mapper);
		IExtendedMappingCollector<NormalizedSimpleStack, Long, IValueArithmetic<BigFraction>> mappingCollector = new LongToBigFractionCollector<>(mapper);

		Path path = Paths.get("config", PECore.MODNAME, "mapping.toml");
		try {
			path.toFile().createNewFile();
		} catch (IOException ex) {
			PECore.LOGGER.error("Couldn't create mapping.toml", ex);
		}

		CommentedFileConfig config = CommentedFileConfig.builder(path).build();
		config.load();

		boolean dumpToFile = getOrSetDefault(config, "general.dumpEverythingToFile", "Want to take a look at the internals of EMC Calculation? Enable this to write all the conversions and setValue-Commands to config/ProjectE/mappingdump.json", false);
		boolean shouldUsePregenerated = getOrSetDefault(config, "general.pregenerate", "When the next EMC mapping occurs write the results to config/ProjectE/pregenerated_emc.json and only ever run the mapping again" +
																					   " when that file does not exist, this setting is set to false, or an error occurred parsing that file.", false);
		boolean logFoundExploits = getOrSetDefault(config, "general.logEMCExploits", "Log known EMC Exploits. This can not and will not find all possible exploits. " +
																					 "This will only find exploits that result in fixed/custom emc values that the algorithm did not overwrite. " +
																					 "Exploits that derive from conversions that are unknown to ProjectE will not be found.", true);

		if (dumpToFile) {
			mappingCollector = new DumpToFileCollector<>(new File(PECore.CONFIG_DIR, "mappingdump.json"), mappingCollector);
		}

		File pregeneratedEmcFile = Paths.get("config", PECore.MODNAME, "pregenerated_emc.json").toFile();
		Map<NormalizedSimpleStack, Long> graphMapperValues;
		if (shouldUsePregenerated && pregeneratedEmcFile.canRead() && PregeneratedEMC.tryRead(pregeneratedEmcFile, graphMapperValues = new HashMap<>())) {
			PECore.LOGGER.info(String.format("Loaded %d values from pregenerated EMC File", graphMapperValues.size()));
		} else {
			SimpleGraphMapper.setLogFoundExploits(logFoundExploits);

			PECore.debugLog("Starting to collect Mappings...");
			for (IEMCMapper<NormalizedSimpleStack, Long> emcMapper : EMC_MAPPERS) {
				try {
					if (getOrSetDefault(config, "enabledMappers." + emcMapper.getName(), emcMapper.getDescription(), emcMapper.isAvailable())) {
						DumpToFileCollector.currentGroupName = emcMapper.getName();
						emcMapper.addMappings(mappingCollector, config, resourceManager);
						PECore.debugLog("Collected Mappings from " + emcMapper.getClass().getName());
					}
				} catch (Exception e) {
					PECore.LOGGER.fatal("Exception during Mapping Collection from Mapper {}. PLEASE REPORT THIS! EMC VALUES MIGHT BE INCONSISTENT!", emcMapper.getClass().getName());
					e.printStackTrace();
				}
			}
			DumpToFileCollector.currentGroupName = "NSSHelper";

			PECore.debugLog("Mapping Collection finished");
			mappingCollector.finishCollection();

			PECore.debugLog("Starting to generate Values:");

			config.save();
			config.close();

			graphMapperValues = valueGenerator.generateValues();
			PECore.debugLog("Generated Values...");

			filterEMCMap(graphMapperValues);

			if (shouldUsePregenerated) {
				//Should have used pregenerated, but the file was not read => regenerate.
				try {
					PregeneratedEMC.write(pregeneratedEmcFile, graphMapperValues);
					PECore.debugLog("Wrote Pregen-file!");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		for (Map.Entry<NormalizedSimpleStack, Long> entry : graphMapperValues.entrySet()) {
			NSSItem normStackItem = (NSSItem) entry.getKey();
			ItemInfo obj = ItemInfo.fromNSS(normStackItem);
			if (obj != null) {
				emc.put(obj, entry.getValue());
			} else {
				PECore.LOGGER.warn("Could not add EMC value for {}, item does not exist!", normStackItem.getResourceLocation());
			}
		}

		MinecraftForge.EVENT_BUS.post(new EMCRemapEvent());
		Transmutation.cacheFullKnowledge();
		FuelMapper.loadMap();
	}

	private static void filterEMCMap(Map<NormalizedSimpleStack, Long> map) {
		map.entrySet().removeIf(e -> !(e.getKey() instanceof NSSItem) || ((NSSItem) e.getKey()).representsTag() || e.getValue() <= 0);
	}

	public static long getEmcValue(IItemProvider item) {
		return getEmcValue(new ItemInfo(item.asItem(), null));
	}

	public static long getEmcValue(ItemInfo info) {
		return emc.get(info);
	}

	public static void clearMaps() {
		emc.clear();
	}
}