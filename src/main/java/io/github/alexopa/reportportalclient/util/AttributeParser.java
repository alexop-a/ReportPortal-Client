/*
 * (C) Copyright 2024 Andreas Alexopoulos (https://alexop-a.github.io/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.github.alexopa.reportportalclient.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.github.alexopa.reportportalclient.rpmodel.ItemAttribute;

/**
 * This class contains functionality for parsing tags and attributes from
 * string. The class has been copied from the <code>client-java</code> project
 * of <a href=
 * "https://github.com/reportportal/client-java/blob/master/src/main/java/com/epam/reportportal/utils/AttributeParser.java">reportportal</a>
 */
public class AttributeParser {

	private static final String ATTRIBUTES_SPLITTER = ";";
	private static final String KEY_VALUE_SPLITTER = ":";

	private AttributeParser() {
		throw new IllegalStateException("Static only class");
	}

	/**
	 * Parse attribute string.<br>
	 * Input attribute string should have format:
	 * build:4r3wf234;attributeKey:attributeValue;attributeValue2;attributeValue3.<br>
	 * Output map should have format:<br>
	 * build:4r3wf234<br>
	 * attributeKey:attributeValue<br>
	 * null:attributeValue2<br>
	 * null:attributeValue3<br>
	 *
	 * @param rawAttributes Attributes string
	 * @return {@link Set} of {@link ItemAttribute}
	 */
	public static Set<ItemAttribute> parseAsSet(String rawAttributes) {
		if (null == rawAttributes) {
			return Collections.emptySet();
		}
		Set<ItemAttribute> attributes = new HashSet<>();

		String[] attributesSplit = rawAttributes.trim().split(ATTRIBUTES_SPLITTER);
		for (String s : attributesSplit) {
			ItemAttribute itemAttributeResource = splitKeyValue(s);
			if (itemAttributeResource != null) {
				attributes.add(itemAttributeResource);
			}
		}
		return attributes;
	}

	/**
	 * Parse a string representation of an attribute to ReportPortal attribute
	 * object instance. E.G.: 'key:value', ' :value', 'tag'
	 *
	 * @param attribute string representation of an attribute
	 * @return ReportPortal attribute object instance
	 */
	public static ItemAttribute splitKeyValue(String attribute) {
		if (null == attribute || attribute.trim().isEmpty()) {
			return null;
		}
		String[] keyValue = attribute.split(KEY_VALUE_SPLITTER);
		if (keyValue.length == 1) {
			return new ItemAttribute(null, keyValue[0].trim());
		} else if (keyValue.length == 2) {
			String key = keyValue[0].trim();
			if (key.isEmpty()) {
				key = null;
			}
			return new ItemAttribute(key, keyValue[1].trim());
		}
		return null;
	}

}
