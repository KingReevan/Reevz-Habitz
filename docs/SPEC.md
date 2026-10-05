# Reevz Habitz — Product Spec

> Source of truth for what the app does. Written by the app's owner; kept verbatim. If an
> implementation question isn't answered here, ask rather than guess.

I am creating a Habit Tracking application. This application will basically help me build habits everyday. It will help me keep track of which habits I completed for the day and which ones I didn't. The app is going to be as minimal as possible. It will only have features which I actually need and no other bloat. 

App Name: `Reevz Habitz`
App Icon: Fist raised up (Signaling Victory).
The Phone in use: `Nothing Phone 2a`

The application will have 7 main screens:
1) `Home`
2) `Menu`
3) `Add Habit`
4) `Remove Habit`
5) `Edit Habit`
6) `Statistics`
7) `Settings`

The entire app will have a header at the top whose contents will change depending on which screen is shown. The header is not very thick, it should be thin and simple.

---
## Home 

The header will just show the current date on the left most side (like '03 October') and the right most side will have two icons. A sort icon and a menu icon (when user clicks it, it will open the `Menu` screen).

Below the header, all the habits will be displayed as a stack of horizontal cards, one after the other with no gaps between the cards. There will be just thin grey lines separating the cards. It should just look like a simple stack. Those cards will have a left edge with an icon on it. Those left edges will also have different colors (the color and icon for each habit is assigned by user in the `Add Habit` section of the app).  The icons will always be pure white against the color of the edge.

After the left edge, the name of the task is displayed. It will have the same font color as the color of the left edge.

On the right side of each card, there will be a checkbox. When user clicks on it, it means that the habit has been completed for today. The habits displayed on the Home screen are only habits that have to performed for TODAY (the current day shown on the header). 

When user clicks on the checkbox of a habit card, then that tick mark will appear on the checkbox and the habit name will appear crossed out. Then that card will appear at the bottom of all the habits.

In short, all the habits that are yet to be complete will be shown at the top and the completed habits will just float to the bottom (after the checkbox is ticked and the habit name is crossed out).

If the name of the habit is too long then it should wrap appropriately (it should not overlap with the checkbox).

The complete habits will be crossed out at the bottom of the screen and their checkboxes will have a tick mark. If the user clicks on the tick mark then a dialog box must open asking if the user wants to mark that habit as 'Not done' for today. If user clicks 'Confirm' then that habit will be active again and will appear along with the active undone habits.

The habits are sorted alphabetically by default. When user clicks on the 'Sort' icon on the header then that habits will be sorted in different ways. I want the sort icon button to toggle between these 3:
1. Alphabetical arrangement
2. Newest to Oldest
3. Oldest to Newest

Every time I click on the sort button, it should toggle between these 3. If the habits are currently arranged as 'Newest to Oldest' and then I click on the sort button, then the arrangement will become 'Oldest to Newest'. If I click on the sort button again it will become 'Alphabetical' and if I click again it will become 'Newest to Oldest' again and so on.

Since the habits shown on the Home screen are only of the current day, their status gets reset after 12:00 am when the new day begins.

The habits must appear every single day in their 'undone' status (unticked).

### All done for the day

After all the habits are ticked for a day, the user should feel good for completing them. When the user ticks the last habit of the day, a star pops up for a moment (a big gold star in the centre of the screen that pops, grows a little and fades out, with a firmer haptic) and then disappears. The star only appears when the user ticks the last habit for the day.

After the star appears, a green card stays pinned at the bottom of the screen that says 'Everything is complete. Fantastic!' The green card must only exist while all habits for the day are done: if the user unticks any habit, it goes away.

---
## Menu Screen

The user will arrive at the menu screen when they click on the Menu icon on the header of the Home screen. The Menu screen looks like the Main Menu page of a video game. It will show Five buttons on the screen. All arranged like a stack. This time the stack will have gaps between the buttons. These are the five buttons:

1. `Add Habit`
2. `Remove Habit`
3. `Edit Habit`
4. `Statistics`
5. `Settings`

The header of the menu screen will just have a arrow on the left most side that when clicked will return the user back to the Home screen. Put breadcrumb next to the arrow so that user knows where they have navigated (it will look like `Home > Menu`).

---
## Add Habit Screen

After user clicks on the `Add Habit` button in the Menu they will be taken to this screen. This `Add Habit` screen is where user is able to create a habit. Any habit created in this section will show up on the Home screen as a card. The user can only create one habit in this screen.

The screen will look like a form with just Five inputs to be taken from the user:
1. Habit Name - Text Field (After user types the name of the habit, it should automatically capitalize the first letter of every word).
2. Description - A description of the habit that will be plain text field
3. Color - The habit will be assigned a color. There must be many good colors to choose from. This color will be the one displayed as the color of the left edge of the habit card on the home screen. And it will also be the color of the habit name displayed on the card.
4. Icon - The habit will be assigned an icon. This icon will be the one displayed on the left edge of the habit on the home screen.
5. Start From - The date when the habit will start showing in the home screen. By default, it shows the next day (tomorrow). User cannot select any past dates. This will be a calendar date selection (Date Picker).

At the bottom right of this screen will be a button labelled 'Create.' When it is clicked, the habit will get created and will be shown in the Home screen on the 'Start From' date (It is like scheduling). After clicking on the 'Create' button, the user will be redirected to the Menu screen. If they want to create another habit, they must click on the `Add Habit` button and repeat the process.

---
## Remove Habit

After user clicks on the `Remove Habit` button in the Menu Screen they will be taken to this screen. This `Remove Habit` screen is where user is able to delete an existing habit. All the habits will be shown to the user just as how it is shown on the Home screen, but with one difference. Here, the user can select multiple habits to delete. The habit cards in this UI will look similar to the cards in the Home Screen along with the checkbox. The only difference here is that the habit cards will also mention the description of each habit on the card. Even the left edge and icon will remain the same. The user can use the checkboxes to select multiple cards. After selecting one or more cards the user will click on the 'Remove Habit(s)' button on the bottom right of the screen. When that button is clicked, a confirmation dialog box will appear. In the confirmation dialog box, there will be a toggle button. The toggle asks if the stats for the deleted habits must be kept or not. This toggle button will be on by default (stats are kept by default). The header for this screen will just have a 'back' button that will take user back to the menu page.

---
## Edit Habit

After user clicks on the `Edit Habit` button in the Menu they will be taken to this screen. This `Edit Habit` screen is where user is able to Edit an existing habit. Any edit made in this section will show up on the Home screen. All the habits will be shown to the user just as how it is shown in the `Remove Habit` screen except for the checkboxes. User can choose a particular habit and edit any of the details related to it. So when they click on a habit they will go to another screen and be able to edit the following for that particular habit:
1. Habit Name
2. Habit Description
3. Color
4. Icon

There will be a 'Save' button on the bottom right. When user clicks it, they will be taken back to the Edit habit screen where all the habits are listed (they can edit another habit if they want).  

When a particular habit is being edited, the header will have a back button in case user wishes to discard any changes made to go back to the screen with all the habits displayed. When that back button is clicked after a change is made then a confirmation dialog box will warn user that the changes will be discarded.

---
## Statistics

After user clicks on the `Statistics` button in the Menu they will be taken to this screen. This `Statistics` screen is where user is able to see statistics for an existing habit or for a habit that has already been deleted. 
 All the habits will be shown to the user just as how it is shown in the `Remove Habit` screen except without the checkboxes (same UI as the `Edit Habit` page). All the deleted habits will also be shown to the user at the bottom (treated as a separate section). User can choose a particular habit and be able to see the statistics related to it. So when they click on, say habit A, they will go to another screen. In this screen, the UI looks like a calendar and user can see everything month-wise only. By default, the current month will be open. Each day will be shown as a circle. 

You must follow this:
1. The days where habit A has been completed = Green circle
2. The days where habit A has not been done = Red circle
3. The future days = greyed out circle
4. The past days before the habit got created = Also red circle
5. Current day must have a yellow ring around the circle to show that it is the current day and it will also follow the same color scheme as the other days (red circle if undone and green if done)

Even deleted habits must follow the same scheme but in their case, when user clicks on them it will first open the month where the user deleted the habit (instead of the current month). Even the year must be shown in this calendar UI even though only month-wise navigation is possible.

Above the calendar UI there will be a thin strip right below the header. This strip will be an accordion (collapsed by default) that will show four statistics related to habit A when expanded:
1. How many days have been completed expressed as the total number of days that the habit had to be carried out. For example, 50/65 would mean that out of the 65 days that I was  supposed to do the habit, I did 50.
2. The current completion streak, example '5 days'
3. The longest completion streak, example '130 days'

Obviously the streak for a habit breaks when one day is missed. Just like Snapchat or Duolingo.

---
## Settings

After user clicks on the `Settings` button in the Menu they will be taken to this screen. This `Settings` screen is where user is able to see configure the settings related to the application. The section will contain the theme toggle of the app. There will be 4 themes: light, dark, vs code dark and tokyo night dark. The tokyo night theme should be reminiscent of the tokyo night (dark) theme of vs code. The vs code dark will be reminiscent of the vs code default dark theme.

Aside from the theme toggle, the settings section will also have an option called 'Clear deleted stats'. On clicking it, it will inform user that the stats of deleted habits will be cleared completely. User will then confirm after the warning and all the statistics associated with previous (now deleted) habits will be deleted from the Database.

The settings section will only have these 2 buttons for now.
