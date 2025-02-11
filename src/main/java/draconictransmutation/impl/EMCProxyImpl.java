package draconictransmutation.impl;

import java.util.Objects;

import javax.annotation.Nonnull;

import draconictransmutation.api.ItemInfo;
import draconictransmutation.api.proxy.IEMCProxy;
import draconictransmutation.emc.nbt.NBTManager;
import draconictransmutation.utils.EMCHelper;

public class EMCProxyImpl implements IEMCProxy {

	public static final EMCProxyImpl instance = new EMCProxyImpl();

	private EMCProxyImpl() {
	}

	@Override
	public long getValue(@Nonnull ItemInfo info) {
		return EMCHelper.getEmcValue(Objects.requireNonNull(info));
	}

	@Override
	public long getSellValue(@Nonnull ItemInfo info) {
		return EMCHelper.getEmcSellValue(Objects.requireNonNull(info));
	}

	@Nonnull
	@Override
	public ItemInfo getPersistentInfo(@Nonnull ItemInfo info) {
		return NBTManager.getPersistentInfo(info);
	}
}