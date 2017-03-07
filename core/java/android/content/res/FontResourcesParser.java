/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.content.res;

import com.android.internal.R;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser for xml type font resources.
 * @hide
 */
public class FontResourcesParser {
    private static final int NORMAL_WEIGHT = 400;
    private static final int ITALIC = 1;

    // A class represents single entry of font-family in xml file.
    public interface FamilyResourceEntry {}

    // A class represents font provider based font-family element in xml file.
    public static final class ProviderResourceEntry implements FamilyResourceEntry {
        private final @NonNull String mProviderAuthority;
        private final @NonNull String mProviderPackage;
        private final @NonNull String mQuery;

        public ProviderResourceEntry(@NonNull String authority, @NonNull String pkg,
                @NonNull String query) {
            mProviderAuthority = authority;
            mProviderPackage = pkg;
            mQuery = query;
        }

        public @NonNull String getAuthority() {
            return mProviderAuthority;
        }

        public @NonNull String getPackage() {
            return mProviderPackage;
        }

        public @NonNull String getQuery() {
            return mQuery;
        }
    }

    // A class represents font element in xml file which points a file in resource.
    public static final class FontFileResourceEntry {
        private final @NonNull String mFileName;
        private int mWeight;
        private boolean mItalic;
        private int mResourceId;

        public FontFileResourceEntry(@NonNull String fileName, int weight, boolean italic,
                int resourceId) {
            mFileName = fileName;
            mWeight = weight;
            mItalic = italic;
            mResourceId = resourceId;
        }

        public @NonNull String getFileName() {
            return mFileName;
        }

        public int getWeight() {
            return mWeight;
        }

        public boolean isItalic() {
            return mItalic;
        }

        public int getResourceId() {
            return mResourceId;
        }
    }

    // A class represents file based font-family element in xml file.
    public static final class FontFamilyFilesResourceEntry implements FamilyResourceEntry {
        private final @NonNull FontFileResourceEntry[] mEntries;

        public FontFamilyFilesResourceEntry(@NonNull FontFileResourceEntry[] entries) {
            mEntries = entries;
        }

        public @NonNull FontFileResourceEntry[] getEntries() {
            return mEntries;
        }
    }

    public static @Nullable FamilyResourceEntry parse(XmlPullParser parser, Resources resources)
            throws XmlPullParserException, IOException {
        int type;
        while ((type=parser.next()) != XmlPullParser.START_TAG
                && type != XmlPullParser.END_DOCUMENT) {
            // Empty loop.
        }

        if (type != XmlPullParser.START_TAG) {
            throw new XmlPullParserException("No start tag found");
        }
        return readFamilies(parser, resources);
    }

    private static @Nullable FamilyResourceEntry readFamilies(XmlPullParser parser,
            Resources resources) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "font-family");
        String tag = parser.getName();
        FamilyResourceEntry result = null;
        if (tag.equals("font-family")) {
            return readFamily(parser, resources);
        } else {
            skip(parser);
            return null;
        }
    }

    private static @Nullable FamilyResourceEntry readFamily(XmlPullParser parser,
            Resources resources) throws XmlPullParserException, IOException {
        AttributeSet attrs = Xml.asAttributeSet(parser);
        TypedArray array = resources.obtainAttributes(attrs, R.styleable.FontFamily);
        String authority = array.getString(R.styleable.FontFamily_fontProviderAuthority);
        String providerPackage = array.getString(R.styleable.FontFamily_fontProviderPackage);
        String query = array.getString(R.styleable.FontFamily_fontProviderQuery);
        array.recycle();
        if (authority != null && providerPackage != null && query != null) {
            while (parser.next() != XmlPullParser.END_TAG) {
                skip(parser);
            }
            return new ProviderResourceEntry(authority, providerPackage, query);
        }
        List<FontFileResourceEntry> fonts = new ArrayList<>();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;
            String tag = parser.getName();
            if (tag.equals("font")) {
                fonts.add(readFont(parser, resources));
            } else {
                skip(parser);
            }
        }
        if (fonts.isEmpty()) {
            return null;
        }
        return new FontFamilyFilesResourceEntry(fonts.toArray(
                new FontFileResourceEntry[fonts.size()]));
    }

    private static FontFileResourceEntry readFont(XmlPullParser parser, Resources resources)
            throws XmlPullParserException, IOException {
        AttributeSet attrs = Xml.asAttributeSet(parser);
        TypedArray array = resources.obtainAttributes(attrs, R.styleable.FontFamilyFont);
        int weight = array.getInt(R.styleable.FontFamilyFont_fontWeight, NORMAL_WEIGHT);
        boolean isItalic = ITALIC == array.getInt(R.styleable.FontFamilyFont_fontStyle, 0);
        String filename = array.getString(R.styleable.FontFamilyFont_font);
        int resourceId = array.getResourceId(R.styleable.FontFamilyFont_font, 0);
        array.recycle();
        while (parser.next() != XmlPullParser.END_TAG) {
            skip(parser);
        }
        return new FontFileResourceEntry(filename, weight, isItalic, resourceId);
    }

    private static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        int depth = 1;
        while (depth > 0) {
            switch (parser.next()) {
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
            }
        }
    }
}
