# Open Decisions

1. **Bonus cap per day**
   - Default: 3
   - Where: `ShakeBonusSelector.MAX_BONUS_PER_DAY`
   - Who decides: Product

2. **Fresh-first ordering**
   - Default: On
   - Where: `FeedConfig.FRESH_FIRST`
   - Who decides: Product

3. **Reset progress keeps theme and mode**
   - Default: Yes
   - Where: `ProgressResetter`
   - Who decides: Product

4. **Surprise mode may repeat yesterday's topic**
   - Default: Allowed
   - Where: `TopicSelector`
   - Who decides: Product

5. **A tap on a past day without a record does nothing**
   - Default: No action
   - Where: `PastDayPolicy`
   - Who decides: Product

6. **First tree visit shows no growth**
   - Default: Yes
   - Where: `TreeGrowth`
   - Who decides: Product

7. **The Revisit chip is not restored after a restart**
   - Default: Lost on process death
   - Where: `FeedViewModel`
   - Who decides: Engineering

8. **Detail opened from a past-day card can complete that card through the Detail rule**
   - Default: Yes
   - Where: `DetailViewModel`
   - Who decides: Product

9. **Plan document correction**
   - Stage threshold for 42 cards is 6, not 5.
   - Where: `TreeConfig`
   - Who decides: Engineering

10. **Submission date**
    - Default: TBD
    - Where: Project plan
    - Who decides: Management
